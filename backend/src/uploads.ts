import {Timestamp} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {createHash,randomUUID} from "node:crypto";
import {db,stamp,callable,participating,localOnly} from "./runtime";
import {fail,id,text,choice} from "./validation";

const hash=(v:unknown)=>createHash("sha256").update(JSON.stringify(v)).digest("hex");
const bytesHash=(v:Buffer)=>createHash("sha256").update(v).digest("hex");
function bucket() {
 if(!process.env.FIREBASE_STORAGE_EMULATOR_HOST) return fail("CONFLICT");
 return getStorage().bucket();
}
function timestamp(v:any):Timestamp|null {
 if(v==null) return null;
 if(!Number.isSafeInteger(v.seconds)||!Number.isInteger(v.nanoseconds)||v.nanoseconds<0||v.nanoseconds>=1e9) return fail("VALIDATION","timestamp");
 try{return new Timestamp(v.seconds,v.nanoseconds);}catch{return fail("VALIDATION","timestamp");}
}
function input(v:any) {
 if(!v || !Number.isInteger(v.price)||v.price<0||v.price>10000000) return fail("VALIDATION","price");
 const optional=(value:unknown,field:string,max:number)=>value==null?null:text(value,field,max)||null;
 return {title:text(v.title,"title",50,1),description:optional(v.description,"description",1000),price:v.price,
 category:choice(v.category,"category",["TEXTBOOK","LIVING","ELECTRONICS","CLOTHING","ETC"]),
 condition:choice(v.condition,"condition",["NEW","GOOD","USED","DEFECTIVE"]),
 place:choice(v.place,"place",["MAIN_GATE","BACK_GATE","LIBRARY","STUDENT_HALL","DORMITORY","ETC"]),
 placeDetail:optional(v.placeDetail,"placeDetail",30),disposeBy:timestamp(v.disposeBy)};
}
function hashes(v:any,max:number):string[] {
 if(!Array.isArray(v)||v.length>max||v.some(h=>typeof h!=="string"||!/^[a-f0-9]{64}$/.test(h))) return fail("VALIDATION","images");
 return v;
}
function productPath(url:unknown):string {
 if(typeof url!=="string") return fail("VALIDATION","photos");
 try {
  const u=new URL(url), prefix="/v0/b/"+bucket().name+"/o/";
  if(!u.pathname.startsWith(prefix)) return fail("VALIDATION","photos");
  const path=decodeURIComponent(u.pathname.slice(prefix.length));
  if(!/^products\/[A-Za-z0-9]{20}\/[0-9]+\.jpg$/.test(path)) return fail("VALIDATION","photos");
  return path;
 }catch{return fail("VALIDATION","photos");}
}
function productRequest(data:any) {
 const value=input(data.input), hs=hashes(data.hashes,15), existing=data.existing??[];
 if(!Array.isArray(existing)||existing.length+hs.length<1||existing.length+hs.length>15) return fail("VALIDATION","images");
 const retained=existing.map(productPath);
 if(new Set(retained).size!==retained.length) return fail("VALIDATION","images");
 const expected=timestamp(data.expectedUpdatedAt);
 const productId=data.productId==null?null:id(data.productId);
 if(productId && !expected) return fail("VALIDATION","expectedUpdatedAt");
 if(!productId && existing.length) return fail("VALIDATION","photos");
 return {value,hs,retained,expected,productId,payloadHash:hash([value,hs,retained,expected,productId])};
}
function ownOperation(op:any,uid:string,payloadHash?:string) {
 if(!op || op.ownerId!==uid) return fail("PERMISSION_DENIED");
 if(payloadHash && op.payloadHash!==payloadHash) return fail("CONFLICT");
 if(op.state==="ABORTED") return fail("CONFLICT");
}
function pending(op:any) {
 if(op.state!=="PREPARED" || Date.now()-op.createdAt.toMillis()>=86400000) return fail("CONFLICT");
}
export const prepareProduct=callable(async(uid,data)=>{
 const requestId=id(data.requestId), r=productRequest(data), ref=db.doc("operations/"+requestId);
 const productId=r.productId??db.collection("products").doc().id, target=db.doc("products/"+productId);
 return db.runTransaction(async tx=>{
  const prior=await tx.get(ref);
  if(prior.exists){ownOperation(prior.data(),uid,r.payloadHash);return prior.data();}
  const [p,asset]=await Promise.all([tx.get(target),tx.get(db.doc("productAssets/"+productId))]);
  if(r.productId && (!p.exists || p.data()!.sellerId!==uid)) return fail("PERMISSION_DENIED");
  if(r.productId && !p.data()!.updatedAt.isEqual(r.expected)) return fail("CONFLICT");
  const start=asset.exists?asset.data()!.nextImageIndex:0;
  const imagePaths=r.hs.map((_,i)=>"products/"+productId+"/"+(start+i)+".jpg");
  const op={operationId:requestId,ownerId:uid,kind:r.productId?"PRODUCT_UPDATE":"PRODUCT_CREATE",targetPath:target.path,
   payloadHash:r.payloadHash,imagePaths,state:"PREPARED",createdAt:stamp(),updatedAt:stamp()};
  tx.set(db.doc("productAssets/"+productId),{sellerId:uid,nextImageIndex:start+r.hs.length});
  tx.create(ref,op);
  return {targetPath:op.targetPath,imagePaths,state:"PREPARED"};
 });
});
export const prepareImages=callable(async(uid,data)=>{
 const chatId=id(data.chatId), requestId=id(data.requestId), hs=hashes(data.hashes,10);
 if(!hs.length) return fail("VALIDATION","images");
 const ref=db.doc("operations/"+requestId), payloadHash=hash([chatId,hs]);
 return db.runTransaction(async tx=>{
  const [room,prior]=await Promise.all([tx.get(db.doc("chatRooms/"+chatId)),tx.get(ref)]);
  participating(room.data(),uid,true);
  if(prior.exists){ownOperation(prior.data(),uid,payloadHash);return prior.data();}
  const imagePaths=hs.map((_,i)=>"chats/"+chatId+"/"+requestId+"_"+i+".jpg");
  const op={operationId:requestId,ownerId:uid,kind:"IMAGE_SEND",targetPath:"chatRooms/"+chatId+"/messages/"+requestId,
   payloadHash,imagePaths,state:"PREPARED",createdAt:stamp(),updatedAt:stamp()};
  tx.create(ref,op); return {targetPath:op.targetPath,imagePaths,state:"PREPARED"};
 });
});
/** Check JPEG markers and dimensions before storing; never trust MIME supplied by a caller. */
function jpeg(bytes:Buffer) {
 if(bytes.length<4||bytes.length>2*1024*1024||bytes[0]!==255||bytes[1]!==216||bytes.at(-2)!==255||bytes.at(-1)!==217) return fail("VALIDATION","image");
 let i=2, valid=false;
 while(i+4<bytes.length) {
  if(bytes[i++]!==255) return fail("VALIDATION","image");
  while(bytes[i]===255)i++;
  const marker=bytes[i++];if(marker===218)break;
  const length=bytes.readUInt16BE(i);if(length<2||i+length>bytes.length) return fail("VALIDATION","image");
  if([192,193,194].includes(marker)){
   if(length<8)return fail("VALIDATION","image");
   const height=bytes.readUInt16BE(i+3),width=bytes.readUInt16BE(i+5);
   if(!height||!width||height>1600||width>1600)return fail("VALIDATION","image");
   valid=true;
  }
  i+=length;
 }
 if(!valid)return fail("VALIDATION","image");
}
export const uploadImage=callable(async(uid,data)=>{
 const ref=db.doc("operations/"+id(data.requestId)), snapshot=await ref.get(), op=snapshot.data();
 ownOperation(op,uid);pending(op);
 if(!Number.isInteger(data.index)||data.index<0||data.index>=op!.imagePaths.length) return fail("VALIDATION","index");
 if(typeof data.base64!=="string"||data.base64.length>2800000||!(/^[A-Za-z0-9+/]*={0,2}$/).test(data.base64)) return fail("VALIDATION","image");
 const bytes=Buffer.from(data.base64,"base64");jpeg(bytes);
 if(op!.kind==="IMAGE_SEND")participating((await db.doc(op!.targetPath.split("/").slice(0,2).join("/")).get()).data(),uid,true);
 const file=bucket().file(op!.imagePaths[data.index]);
 const metadata:any={contentType:"image/jpeg",metadata:{contentHash:bytesHash(bytes)}};
 if(op!.kind!=="IMAGE_SEND")metadata.metadata.firebaseStorageDownloadTokens=randomUUID();
 try { await file.save(bytes,{resumable:false,preconditionOpts:{ifGenerationMatch:0},metadata}); }
 catch(error:any) {
  if(error.code!==412)throw error;
  const [stored]=await file.download();if(!stored.equals(bytes))return fail("CONFLICT");
 }
 const latest=(await ref.get()).data();ownOperation(latest,uid);pending(latest);
 return null;
});
async function uploaded(paths:string[],hs:string[]) {
 for(let i=0;i<paths.length;i++){
  const [bytes]=await bucket().file(paths[i]).download();
  if(bytesHash(bytes)!==hs[i])return fail("CONFLICT");
 }
}
async function productUrl(path:string) {
 const [metadata]=await bucket().file(path).getMetadata();
 const token=metadata.metadata?.firebaseStorageDownloadTokens;
 if(typeof token!=="string")return fail("CONFLICT");
 return "http://"+process.env.FIREBASE_STORAGE_EMULATOR_HOST+"/v0/b/"+bucket().name+"/o/"+encodeURIComponent(path)+"?alt=media&token="+token;
}
function deadline(value:Timestamp|null,old:Timestamp|null) {
 if(!value || (old && value.isEqual(old)))return;
 const offset=9*3600000, day=86400000, millis=value.toMillis();
 if((millis+offset)%day!==0 || millis<Math.floor((Date.now()+offset)/day)*day-offset+day)fail("VALIDATION","disposeBy");
}
export const commitProduct=callable(async(uid,data)=>{
 const r=productRequest(data), ref=db.doc("operations/"+id(data.requestId));
 const snapshot=await ref.get(), op=snapshot.data();ownOperation(op,uid,r.payloadHash);
 if(op!.state==="COMMITTED"||op!.state==="CLEANUP_REQUIRED")return op!.targetPath.split("/")[1];
 pending(op);await uploaded(op!.imagePaths,r.hs);
 const urls=await Promise.all(op!.imagePaths.map(productUrl));
 const productId=op!.targetPath.split("/")[1], target=db.doc(op!.targetPath);
 await db.runTransaction(async tx=>{
  const [current,operation,profile]=await Promise.all([tx.get(target),tx.get(ref),tx.get(db.doc("users/"+uid))]);
  const operationData=operation.data();ownOperation(operationData,uid,r.payloadHash);
  if(operationData!.state!=="PREPARED") { if(operationData!.state==="COMMITTED"||operationData!.state==="CLEANUP_REQUIRED")return;return fail("CONFLICT"); }
  pending(operationData);
  let kept:string[]=[],removed:string[]=[];
  if(r.productId){
   if(!current.exists || current.data()!.sellerId!==uid)return fail("PERMISSION_DENIED");
   if(!current.data()!.updatedAt.isEqual(r.expected))return fail("CONFLICT");
   const originals:string[]=current.data()!.imageUrls, originalPaths=originals.map(productPath);
   const chosen=originalPaths.filter(p=>r.retained.includes(p));
   if(JSON.stringify(chosen)!==JSON.stringify(r.retained))return fail("VALIDATION","photos");
   kept=originals.filter((_,i)=>r.retained.includes(originalPaths[i]));
   removed=originalPaths.filter(p=>!r.retained.includes(p));
  }else if(current.exists)return fail("CONFLICT");
  deadline(r.value.disposeBy,current.data()?.disposeBy??null);
  if(!profile.exists)return fail("INCOMPLETE_PROFILE");
  tx.set(target,{...r.value,productId,sellerId:uid,sellerNickname:profile.data()!.nickname,imageUrls:[...kept,...urls],
   status:current.data()?.status??"SELLING",createdAt:current.data()?.createdAt??stamp(),updatedAt:stamp()});
  tx.update(ref,{state:removed.length?"CLEANUP_REQUIRED":"COMMITTED",imagePaths:removed,updatedAt:stamp()});
 });
 await cleanOperation(ref).catch(()=>undefined);
 return productId;
});
export const commitImages=callable(async(uid,data)=>{
 const chatId=id(data.chatId), requestId=id(data.requestId), hs=hashes(data.hashes,10);
 const ref=db.doc("operations/"+requestId), snapshot=await ref.get(), op=snapshot.data();
 const payloadHash=hash([chatId,hs]);ownOperation(op,uid,payloadHash);
 if(op!.state!=="COMMITTED"){pending(op);await uploaded(op!.imagePaths,hs);}
 const roomRef=db.doc("chatRooms/"+chatId), message=roomRef.collection("messages").doc(requestId);
 await db.runTransaction(async tx=>{
  const [room,operation]=await Promise.all([tx.get(roomRef),tx.get(ref)]);
  const r=room.data(), o=operation.data();participating(r,uid,true);ownOperation(o,uid,payloadHash);
  if(o!.state==="COMMITTED")return;
  pending(o);const recipient=r!.sellerId===uid?r!.buyerId:r!.sellerId, sequence=r!.lastSequence+1;
  tx.create(message,{messageId:requestId,senderId:uid,type:"IMAGE",text:null,imageUrls:o!.imagePaths.map((p:string)=>"gs://"+bucket().name+"/"+p),sequence,createdAt:stamp()});
  tx.update(roomRef,{lastSequence:sequence,lastMessage:"사진",lastMessageAt:stamp(),unreadCount:{...r!.unreadCount,[recipient]:r!.unreadCount[recipient]+1}});
  tx.update(ref,{state:"COMMITTED",updatedAt:stamp()});
 });
 return null;
});
export const deleteProduct=callable(async(uid,data)=>{
 const productId=id(data.productId), target=db.doc("products/"+productId), op=db.collection("operations").doc();
 await db.runTransaction(async tx=>{
  const [p,asset]=await Promise.all([tx.get(target),tx.get(db.doc("productAssets/"+productId))]);
  if(!p.exists){if(asset.data()?.sellerId!==uid)return fail("PERMISSION_DENIED");return;}
  if(p.data()!.sellerId!==uid)return fail("PERMISSION_DENIED");
  tx.delete(target);
  tx.create(op,{operationId:op.id,ownerId:uid,kind:"PRODUCT_DELETE",targetPath:target.path,payloadHash:hash(productId),
   imagePaths:p.data()!.imageUrls.map(productPath),state:"CLEANUP_REQUIRED",createdAt:stamp(),updatedAt:stamp()});
 });
 await cleanOperation(op).catch(()=>undefined);return null;
});
async function cleanOperation(ref:FirebaseFirestore.DocumentReference) {
 const snapshot=await ref.get();if(!snapshot.exists)return;
 let op=snapshot.data()!;
 if(op.state==="PREPARED" && Date.now()-op.createdAt.toMillis()>=86400000){
  await db.runTransaction(async tx=>{
   const latest=(await tx.get(ref)).data()!;
   if(latest.state==="PREPARED" && Date.now()-latest.createdAt.toMillis()>=86400000)tx.update(ref,{state:"ABORTED",updatedAt:stamp()});
  });
  op=(await ref.get()).data()!;
 }
 if(op.state!=="CLEANUP_REQUIRED" && op.state!=="ABORTED")return;
 for(const path of op.imagePaths)await bucket().file(path).delete({ignoreNotFound:true});
 if(op.state==="CLEANUP_REQUIRED")await ref.update({state:"COMMITTED",imagePaths:[],updatedAt:stamp()});
 // Retain aborted records: a previously in-flight upload can finish after the first cleanup.
}
export async function cleanup() {
 localOnly();
 const all=await db.collection("operations").where("state","in",["PREPARED","CLEANUP_REQUIRED","ABORTED"]).get();
 for(const op of all.docs)await cleanOperation(op.ref);
}
