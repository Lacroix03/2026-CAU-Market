const {test,before,after}=require("node:test");
const assert=require("node:assert/strict");
const admin=require("firebase-admin/app"), adb=require("firebase-admin/firestore");
const {initializeApp,deleteApp}=require("firebase/app");
const {getAuth,connectAuthEmulator,createUserWithEmailAndPassword}=require("firebase/auth");
const {getFunctions,connectFunctionsEmulator,httpsCallable}=require("firebase/functions");
const {getFirestore,connectFirestoreEmulator,doc,getDoc,setDoc,collection,query,where,getDocs,terminate}=require("firebase/firestore");
const {getStorage,connectStorageEmulator,ref,getDownloadURL,uploadBytes}=require("firebase/storage");
if(!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST)
 throw Error("Emulators required");
const server=admin.initializeApp({projectId:"demo-cau-market"},"tests");
const db=adb.getFirestore(server), clients=[];
async function client(label) {
 const app=initializeApp({projectId:"demo-cau-market",apiKey:"demo-key",appId:"demo-app",storageBucket:"demo-cau-market.appspot.com"},label+Date.now());
 const auth=getAuth(app);connectAuthEmulator(auth,"http://127.0.0.1:9099",{disableWarnings:true});
 const functions=getFunctions(app,"asia-northeast3");connectFunctionsEmulator(functions,"127.0.0.1",5001);
 const store=getFirestore(app);connectFirestoreEmulator(store,"127.0.0.1",8080);
 const storage=getStorage(app);connectStorageEmulator(storage,"127.0.0.1",9199);
 const email=label+Date.now()+"@cau.ac.kr";
 const account=await createUserWithEmailAndPassword(auth,email,"DemoOnly123!");
 const c={app,store,storage,uid:account.user.uid,call:async(name,data)=>(await httpsCallable(functions,name)(data)).data};
 clients.push(c);await c.call("completeSignUp",{nickname:"사용자"});return c;
}
let seller,buyer,outsider;
before(async()=>{seller=await client("seller");buyer=await client("buyer");outsider=await client("outsider");});
after(async()=>{for(const c of clients){await terminate(c.store);await deleteApp(c.app);}await admin.deleteApp(server);});
async function product() {
 const p=db.collection("products").doc(), t=adb.Timestamp.now();
 await p.set({productId:p.id,sellerId:seller.uid,sellerNickname:"사용자",title:"교재",description:null,price:0,category:"TEXTBOOK",condition:"GOOD",imageUrls:["http://localhost/image.jpg"],place:"LIBRARY",placeDetail:null,disposeBy:null,status:"SELLING",createdAt:t,updatedAt:t});
 return p;
}
test("private profiles, products and internal writes enforce access",async()=>{
 assert.equal((await getDoc(doc(buyer.store,"users",buyer.uid))).exists(),true);
 await assert.rejects(getDoc(doc(buyer.store,"users",seller.uid)));
 const p=await product();assert.equal((await getDoc(doc(buyer.store,"products",p.id))).exists(),true);
 await assert.rejects(setDoc(doc(buyer.store,"products",p.id),{title:"tamper"},{merge:true}));
 await assert.rejects(getDoc(doc(buyer.store,"activeChatKeys","unknown")));
 await assert.rejects(uploadBytes(ref(buyer.storage,"chats/unknown/unknown_0.jpg"),new Uint8Array([255,216,255,217]),{contentType:"image/jpeg"}));
 await assert.rejects(getDownloadURL(ref(buyer.storage,"chats/unknown/unknown_0.jpg")));
});
test("concurrent creation, retries, read boundary, leave and new room",async()=>{
 const p=await product();
 const ids=await Promise.all(Array.from({length:6},()=>buyer.call("getOrCreateChatRoom",{productId:p.id})));
 assert.equal(new Set(ids).size,1);const chatId=ids[0],room=db.doc("chatRooms/"+chatId);
 await assert.rejects(setDoc(doc(buyer.store,"chatRooms",chatId),{unreadCount:{[seller.uid]:0}},{merge:true}));
 await assert.rejects(setDoc(doc(buyer.store,"chatRooms",chatId,"messages",db.collection("_").doc().id),
  {senderId:"system",type:"SYSTEM",text:"위조",imageUrls:[],sequence:1,createdAt:require("firebase/firestore").Timestamp.now()}));
 await assert.rejects(outsider.call("sendMessage",{chatId,text:"x",requestId:db.collection("_").doc().id}));
 await assert.rejects(buyer.call("sendMessage",{chatId,text:" ",requestId:db.collection("_").doc().id}));
 const requestId=db.collection("_").doc().id;
 await Promise.all(Array.from({length:6},()=>buyer.call("sendMessage",{chatId,text:"문의",requestId})));
 assert.equal((await room.collection("messages").get()).size,1);
 assert.equal((await room.get()).data().unreadCount[seller.uid],1);
 await assert.rejects(buyer.call("sendMessage",{chatId,text:"다른 내용",requestId}));
 const second=db.collection("_").doc().id;
 await Promise.all([seller.call("markAsRead",{chatId,throughMessageId:requestId}),buyer.call("sendMessage",{chatId,text:"추가",requestId:second})]);
 let value=(await room.get()).data();
 assert.equal(value.unreadCount[seller.uid],1);assert.equal(value.lastReadSequence[seller.uid],1);
 await seller.call("markAsRead",{chatId,throughMessageId:second});
 assert.equal((await room.get()).data().unreadCount[seller.uid],0);
 await assert.rejects(outsider.call("getChatRoomInfo",{chatId}));
 await assert.rejects(getDoc(doc(outsider.store,"chatRooms",chatId)));
 await seller.call("changeProductStatus",{productId:p.id,status:"SOLD"});
 assert.equal(await buyer.call("getOrCreateChatRoom",{productId:p.id}),chatId);
 await assert.rejects(outsider.call("getOrCreateChatRoom",{productId:p.id}));
 await buyer.call("leaveChatRoom",{chatId});await buyer.call("leaveChatRoom",{chatId});
 assert.equal((await room.collection("messages").where("type","==","SYSTEM").get()).size,1);
 await assert.rejects(getDoc(doc(buyer.store,"chatRooms",chatId)));
 assert.equal((await getDoc(doc(seller.store,"chatRooms",chatId))).exists(),true);
 await assert.rejects(seller.call("sendMessage",{chatId,text:"금지",requestId:db.collection("_").doc().id}));
 await assert.rejects(buyer.call("getOrCreateChatRoom",{productId:p.id}));
 await seller.call("changeProductStatus",{productId:p.id,status:"RESERVED"});
 const next=await buyer.call("getOrCreateChatRoom",{productId:p.id});assert.notEqual(next,chatId);
 assert.equal((await db.doc("chatRooms/"+next).collection("messages").get()).size,0);
 const visible=await getDocs(query(collection(buyer.store,"chatRooms"),where("activeUserIds","array-contains",buyer.uid)));
 assert(!visible.docs.some(d=>d.id===chatId));
 await p.delete();
 assert.equal((await buyer.call("getChatRoomInfo",{chatId:next})).product,null);
 await buyer.call("sendMessage",{chatId:next,text:"삭제 후 기존 대화",requestId:db.collection("_").doc().id});
});



test("product validation, immutable uploads, edit conflicts and cleanup",async()=>{
 const fs=require("node:fs"),crypto=require("node:crypto");
 const base64=fs.readFileSync(__dirname+"/image.base64","utf8"), bytes=Buffer.from(base64,"base64");
 const digest=crypto.createHash("sha256").update(bytes).digest("hex");
 const input={title:" 교재 ",description:" ",price:0,category:"TEXTBOOK",condition:"GOOD",place:"LIBRARY",placeDetail:null,disposeBy:null};
 const request={requestId:db.collection("_").doc().id,input,existing:[],hashes:[digest,digest]};
 await assert.rejects(seller.call("prepareProduct",{...request,input:{...input,price:10000001}}));
 await assert.rejects(seller.call("prepareProduct",{...request,input:{...input,title:"x".repeat(51)}}));
 const prepared=await seller.call("prepareProduct",request);
 await assert.rejects(seller.call("prepareProduct",{...request,hashes:[]}));
 await assert.rejects(seller.call("prepareProduct",{...request,hashes:Array(16).fill(digest)}));
 await assert.rejects(outsider.call("uploadImage",{requestId:request.requestId,index:0,base64}));
 await assert.rejects(seller.call("uploadImage",{requestId:request.requestId,index:0,base64:Buffer.from("not jpeg").toString("base64")}));
 await seller.call("uploadImage",{requestId:request.requestId,index:0,base64});
 await assert.rejects(seller.call("commitProduct",request));
 await seller.call("uploadImage",{requestId:request.requestId,index:1,base64});
 const productId=await seller.call("commitProduct",request);
 assert.equal(await seller.call("commitProduct",request),productId);
 const ref=db.doc("products/"+productId), initial=(await ref.get()).data();
 assert.equal(initial.title,"교재");assert.equal(initial.description,null);assert.equal(initial.status,"SELLING");
 assert.equal(initial.imageUrls.length,2);
 const timestamp=t=>({seconds:t.seconds,nanoseconds:t.nanoseconds});
 const update={requestId:db.collection("_").doc().id,productId,input:{...input,title:"수정"},
  expectedUpdatedAt:timestamp(initial.updatedAt),existing:[initial.imageUrls[1]],hashes:[]};
 await seller.call("prepareProduct",update);await seller.call("commitProduct",update);
 assert.deepEqual((await ref.get()).data().imageUrls,[initial.imageUrls[1]]);
 await assert.rejects(seller.call("prepareProduct",{...update,requestId:db.collection("_").doc().id}));
 const current=(await ref.get()).data();
 const today=Math.floor((Date.now()+9*3600000)/86400000)*86400000-9*3600000;
 const edit=disposeBy=>({requestId:db.collection("_").doc().id,productId,
  input:{...input,disposeBy},expectedUpdatedAt:timestamp(current.updatedAt),existing:current.imageUrls,hashes:[]});
 const invalidDate=edit(timestamp(adb.Timestamp.fromMillis(today)));
 await seller.call("prepareProduct",invalidDate);await assert.rejects(seller.call("commitProduct",invalidDate));
 const tomorrow=edit(timestamp(adb.Timestamp.fromMillis(today+86400000)));
 await seller.call("prepareProduct",tomorrow);await seller.call("commitProduct",tomorrow);
 assert.equal((await ref.get()).data().disposeBy.toMillis(),today+86400000);
 // An unchanged expired deadline remains editable; no waiting for a real midnight is needed.
 await ref.update({disposeBy:adb.Timestamp.fromMillis(today-86400000)});
 const expired=(await ref.get()).data(), unchanged={...edit(timestamp(expired.disposeBy)),expectedUpdatedAt:timestamp(expired.updatedAt)};
 await seller.call("prepareProduct",unchanged);await seller.call("commitProduct",unchanged);
 await assert.rejects(outsider.call("changeProductStatus",{productId,status:"SOLD"}));
 const chatId=await buyer.call("getOrCreateChatRoom",{productId});
 const storage=require("firebase-admin/storage").getStorage(server).bucket("demo-cau-market.appspot.com");
 assert.equal((await storage.file(prepared.imagePaths[0]).exists())[0],false);
 await seller.call("deleteProduct",{productId});
 await seller.call("deleteProduct",{productId});
 assert.equal((await ref.get()).exists,false);
 assert.equal((await buyer.call("getChatRoomInfo",{chatId})).product,null);
 assert.equal((await storage.file(prepared.imagePaths[1]).exists())[0],false);
});
test("chat images require active membership, have no tokens, and deduplicate",async()=>{
 const fs=require("node:fs"),crypto=require("node:crypto");
 const base64=fs.readFileSync(__dirname+"/image.base64","utf8"), bytes=Buffer.from(base64,"base64");
 const digest=crypto.createHash("sha256").update(bytes).digest("hex");
 const p=await product(),chatId=await buyer.call("getOrCreateChatRoom",{productId:p.id});
 const request={chatId,requestId:db.collection("_").doc().id,hashes:[digest]};
 await assert.rejects(buyer.call("prepareImages",{...request,hashes:[]}));
 await assert.rejects(buyer.call("prepareImages",{...request,hashes:Array(11).fill(digest)}));
 await buyer.call("prepareImages",request);
 await buyer.call("uploadImage",{requestId:request.requestId,index:0,base64});
 await Promise.all([buyer.call("commitImages",request),buyer.call("commitImages",request)]);
 const room=db.doc("chatRooms/"+chatId), message=(await room.collection("messages").doc(request.requestId).get()).data();
 assert.equal(message.type,"IMAGE");assert.equal(message.text,null);
 assert.equal((await room.get()).data().unreadCount[seller.uid],1);
 assert.equal((await seller.call("getChatImage",{chatId,messageId:request.requestId,index:0})).base64,base64);
 await assert.rejects(outsider.call("getChatImage",{chatId,messageId:request.requestId,index:0}));
 const path="chats/"+chatId+"/"+request.requestId+"_0.jpg";
 await assert.rejects(getDownloadURL(ref(buyer.storage,path)));
 const storage=require("firebase-admin/storage").getStorage(server).bucket("demo-cau-market.appspot.com");
 assert.equal((await storage.file(path).getMetadata())[0].metadata?.firebaseStorageDownloadTokens,undefined);
 await buyer.call("leaveChatRoom",{chatId});
 await assert.rejects(buyer.call("getChatImage",{chatId,messageId:request.requestId,index:0}));
 assert.equal((await seller.call("getChatImage",{chatId,messageId:request.requestId,index:0})).base64,base64);
});
test("non-school signup is rejected before registration",async()=>{
 const app=initializeApp({projectId:"demo-cau-market",apiKey:"demo-key",appId:"demo-app"},"external");
 const auth=getAuth(app);connectAuthEmulator(auth,"http://127.0.0.1:9099",{disableWarnings:true});
 await assert.rejects(createUserWithEmailAndPassword(auth,"outsider@example.com","DemoOnly123!"));
 await deleteApp(app);
});

test("expired upload cleanup retains tombstones and removes late orphan files",async()=>{
 const fs=require("node:fs"),crypto=require("node:crypto");
 const base64=fs.readFileSync(__dirname+"/image.base64","utf8"),bytes=Buffer.from(base64,"base64");
 const digest=crypto.createHash("sha256").update(bytes).digest("hex");
 const request={requestId:db.collection("_").doc().id,input:{title:"만료",description:null,price:1,category:"ETC",condition:"NEW",place:"ETC",placeDetail:null,disposeBy:null},existing:[],hashes:[digest]};
 const prepared=await seller.call("prepareProduct",request);
 await seller.call("uploadImage",{requestId:request.requestId,index:0,base64});
 await db.doc("operations/"+request.requestId).update({createdAt:adb.Timestamp.fromMillis(Date.now()-2*86400000)});
 const {cleanup}=require("../lib/uploads");
 await cleanup();
 const storage=require("firebase-admin/storage").getStorage(server).bucket("demo-cau-market.appspot.com");
 assert.equal((await db.doc("operations/"+request.requestId).get()).data().state,"ABORTED");
 assert.equal((await storage.file(prepared.imagePaths[0]).exists())[0],false);
 await assert.rejects(seller.call("commitProduct",request));
 await storage.file(prepared.imagePaths[0]).save(bytes,{metadata:{contentType:"image/jpeg"}});
 await cleanup();
 assert.equal((await storage.file(prepared.imagePaths[0]).exists())[0],false);
 await adb.getFirestore().terminate();
 await admin.deleteApp(admin.getApp());
});

test("anonymous and non-school identities cannot read data or write counters",async()=>{
 const {initializeTestEnvironment}=require("@firebase/rules-unit-testing");
 const env=await initializeTestEnvironment({projectId:"demo-cau-market",firestore:{host:"127.0.0.1",port:8080}});
 try {
  const p=await product();
  for(const context of [env.unauthenticatedContext(),env.authenticatedContext("external-rule-user",{email:"x@other.cau.ac.kr"})]) {
   const firestore=context.firestore();
   await assert.rejects(firestore.doc("products/"+p.id).get());
   await assert.rejects(firestore.doc("users/"+seller.uid).get());
   await assert.rejects(firestore.doc("operations/invalid").set({state:"COMMITTED"}));
  }
 } finally {await env.cleanup();}
});
