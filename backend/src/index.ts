import {onCall, HttpsError} from "firebase-functions/v2/https";
import {beforeUserCreated, beforeUserSignedIn} from "firebase-functions/v2/identity";
import {createHash} from "node:crypto";
import {getStorage} from "firebase-admin/storage";
import {fail,id,text,schoolEmail,choice} from "./validation";
import {db,stamp,localOnly,callable,participating} from "./runtime";
const checkEmail=(event:any)=>{if(!schoolEmail(event.data?.email)) fail("PERMISSION_DENIED");};
export const restrictSignUp=beforeUserCreated(checkEmail);
export const restrictSignIn=beforeUserSignedIn(checkEmail);
export const completeSignUp=onCall({region:"asia-northeast3"},async request=>{
 localOnly();
 if(!request.auth || !schoolEmail(request.auth.token.email)) return fail("AUTH_REQUIRED");
 const nickname=text(request.data?.nickname,"nickname",10,2);
 const ref=db.doc("users/"+request.auth.uid);
 await db.runTransaction(async tx=>{
  if((await tx.get(ref)).exists) return;
  tx.create(ref,{userId:request.auth!.uid,email:request.auth!.token.email!.toLowerCase(),nickname,createdAt:stamp()});
 });
 return null;
});
const key=(productId:string,buyerId:string)=>createHash("sha256").update(productId+"|"+buyerId).digest("hex");
export const getOrCreateChatRoom=callable(async(uid,data)=>{
 const productId=id(data.productId), slot=db.doc("activeChatKeys/"+key(productId,uid));
 const fresh=db.collection("chatRooms").doc();
 return db.runTransaction(async tx=>{
  const lock=await tx.get(slot);
  if(lock.exists) {
   const previous=await tx.get(db.doc("chatRooms/"+lock.data()!.chatId));
   if(previous.exists && previous.data()!.activeUserIds.length===2) {
    participating(previous.data(),uid,true); return previous.id;
   }
  }
  const product=await tx.get(db.doc("products/"+productId));
  if(!product.exists) return fail("NOT_FOUND");
  const p=product.data()!;
  if(p.sellerId===uid || p.status==="SOLD") return fail("PERMISSION_DENIED");
  tx.create(fresh,{chatId:fresh.id,productId,sellerId:p.sellerId,buyerId:uid,activeUserIds:[p.sellerId,uid],
   lastMessage:null,lastMessageAt:null,unreadCount:{[p.sellerId]:0,[uid]:0},lastReadAt:{},
   lastSequence:0,lastReadSequence:{[p.sellerId]:0,[uid]:0},createdAt:stamp()});
  tx.set(slot,{productId,buyerId:uid,chatId:fresh.id,updatedAt:stamp()});
  return fresh.id;
 });
});
export const sendMessage=callable(async(uid,data)=>{
 const chatId=id(data.chatId), operationId=id(data.requestId), body=text(data.text,"text",1000,1);
 const roomRef=db.doc("chatRooms/"+chatId), messageRef=roomRef.collection("messages").doc(operationId);
 const operation=db.doc("operations/"+operationId);
 const hash=createHash("sha256").update(JSON.stringify([chatId,body])).digest("hex");
 await db.runTransaction(async tx=>{
  const [snapshot,op]=await Promise.all([tx.get(roomRef),tx.get(operation)]);
  if(!snapshot.exists) return fail("NOT_FOUND");
  const room=snapshot.data()!;
  participating(room,uid,true);
  if(op.exists) {
   if(op.data()!.ownerId!==uid || op.data()!.payloadHash!==hash || op.data()!.kind!=="TEXT_SEND") return fail("CONFLICT");
   return;
  }
  const recipient=room.sellerId===uid?room.buyerId:room.sellerId;
  const sequence=room.lastSequence+1;
  tx.create(messageRef,{messageId:operationId,senderId:uid,type:"TEXT",text:body,imageUrls:[],createdAt:stamp(),sequence});
  tx.update(roomRef,{lastMessage:body,lastMessageAt:stamp(),lastSequence:sequence,unreadCount:{...room.unreadCount,[recipient]:room.unreadCount[recipient]+1}});
  tx.create(operation,{operationId,ownerId:uid,kind:"TEXT_SEND",targetPath:messageRef.path,payloadHash:hash,imagePaths:[],state:"COMMITTED",createdAt:stamp(),updatedAt:stamp()});
 });
 return null;
});
export const markAsRead=callable(async(uid,data)=>{
 const ref=db.doc("chatRooms/"+id(data.chatId)), messageId=id(data.throughMessageId);
 await db.runTransaction(async tx=>{
  const [snapshot,boundary]=await Promise.all([tx.get(ref),tx.get(ref.collection("messages").doc(messageId))]);
  if(!snapshot.exists || !boundary.exists) return fail("NOT_FOUND");
  const room=snapshot.data()!, message=boundary.data()!;
  participating(room,uid);
  const previous=room.lastReadSequence[uid]??0;
  if(message.sequence<=previous) return;
  const received=await tx.get(ref.collection("messages").where("sequence",">",previous).where("sequence","<=",message.sequence));
  const count=received.docs.filter(d=>d.data().senderId!==uid && d.data().type!=="SYSTEM").length;
  if(room.unreadCount[uid]<count) return fail("CONFLICT");
  tx.update(ref,{unreadCount:{...room.unreadCount,[uid]:room.unreadCount[uid]-count},
   lastReadSequence:{...room.lastReadSequence,[uid]:message.sequence},lastReadAt:{...room.lastReadAt,[uid]:message.createdAt}});
 });
 return null;
});
export const leaveChatRoom=callable(async(uid,data)=>{
 const ref=db.doc("chatRooms/"+id(data.chatId)), system=ref.collection("messages").doc();
 await db.runTransaction(async tx=>{
  const snapshot=await tx.get(ref);
  if(!snapshot.exists) return fail("NOT_FOUND");
  const room=snapshot.data()!;
  if(uid!==room.sellerId && uid!==room.buyerId) return fail("PERMISSION_DENIED");
  if(!room.activeUserIds.includes(uid)) return;
  const slot=db.doc("activeChatKeys/"+key(room.productId,room.buyerId)), lock=await tx.get(slot);
  const update:any={activeUserIds:room.activeUserIds.filter((v:string)=>v!==uid)};
  if(room.activeUserIds.length===2) {
   const body="상대방이 채팅방을 나갔습니다", sequence=room.lastSequence+1;
   tx.create(system,{messageId:system.id,senderId:"system",type:"SYSTEM",text:body,imageUrls:[],createdAt:stamp(),sequence});
   Object.assign(update,{lastSequence:sequence,lastMessage:body,lastMessageAt:stamp()});
  }
  tx.update(ref,update);
  if(lock.exists && lock.data()!.chatId===ref.id) tx.delete(slot);
 });
 return null;
});
export const changeProductStatus=callable(async(uid,data)=>{
 const ref=db.doc("products/"+id(data.productId)), status=choice(data.status,"status",["SELLING","RESERVED","SOLD"]);
 await db.runTransaction(async tx=>{
  const p=await tx.get(ref);
  if(!p.exists) return fail("NOT_FOUND");
  if(p.data()!.sellerId!==uid) return fail("PERMISSION_DENIED");
  tx.update(ref,{status,updatedAt:stamp()});
 });
 return null;
});
export const getChatRoomInfo=callable(async(uid,data)=>{
 const room=await db.doc("chatRooms/"+id(data.chatId)).get();
 if(!room.exists) return fail("NOT_FOUND");
 const r=room.data()!; participating(r,uid);
 const opponentId=r.sellerId===uid?r.buyerId:r.sellerId;
 const [opponent,product]=await Promise.all([db.doc("users/"+opponentId).get(),db.doc("products/"+r.productId).get()]);
 if(!opponent.exists) return fail("NOT_FOUND");
 return {opponentId,opponentNickname:opponent.data()!.nickname,product:product.exists?product.data():null};
});

export const getChatImage=callable(async(uid,data)=>{
 if(!process.env.FIREBASE_STORAGE_EMULATOR_HOST) throw new HttpsError("failed-precondition","Storage emulator required");
 const chatId=id(data.chatId), messageId=id(data.messageId), index=data.index;
 if(!Number.isInteger(index) || index<0 || index>=10) return fail("VALIDATION","index");
 const roomRef=db.doc("chatRooms/"+chatId);
 const [room,message]=await Promise.all([roomRef.get(),roomRef.collection("messages").doc(messageId).get()]);
 if(!room.exists || !message.exists) return fail("NOT_FOUND");
 participating(room.data(),uid);
 const path="chats/"+chatId+"/"+messageId+"_"+index+".jpg";
 const bucket=getStorage().bucket();
 if(message.data()!.type!=="IMAGE" || message.data()!.imageUrls[index]!=="gs://"+bucket.name+"/"+path)
  return fail("NOT_FOUND");
 const file=bucket.file(path);
 const [metadata]=await file.getMetadata();
 if(Number(metadata.size)>2*1024*1024 || metadata.contentType!=="image/jpeg") return fail("CONFLICT");
 const [bytes]=await file.download();
 const latest=await roomRef.get();
 participating(latest.data(),uid);
 return {base64:bytes.toString("base64")};
});
export {prepareProduct,prepareImages,uploadImage,commitProduct,commitImages,deleteProduct} from "./uploads";
