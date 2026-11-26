// Run only through firebase emulators:exec --import after the backend test export.
const assert=require("node:assert/strict");
if(!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST || !process.env.FIREBASE_STORAGE_EMULATOR_HOST)
 throw Error("All local emulators are required");
const {initializeApp,deleteApp}=require("firebase-admin/app");
const {getFirestore}=require("firebase-admin/firestore");
const {getAuth}=require("firebase-admin/auth");
const {getStorage}=require("firebase-admin/storage");
const app=initializeApp({projectId:"demo-cau-market",storageBucket:"demo-cau-market.appspot.com"});
(async()=>{
 const db=getFirestore(app);
 try {
  assert((await getAuth(app).listUsers()).users.length>=3);
  assert((await db.collection("users").get()).size>=3);
  const images=await db.collectionGroup("messages").where("type","==","IMAGE").get();
  assert(images.size>0);
  for(const message of images.docs) {
   for(const reference of message.data().imageUrls) {
    const prefix="gs://demo-cau-market.appspot.com/";
    assert(reference.startsWith(prefix));
    const file=getStorage(app).bucket().file(reference.slice(prefix.length));
    assert((await file.download())[0].length>0);
    assert.equal((await file.getMetadata())[0].metadata?.firebaseStorageDownloadTokens,undefined);
   }
  }
  console.log("IMPORT PASS: Auth accounts, Firestore messages and private Storage image bytes restored.");
 } finally {await db.terminate();await deleteApp(app);}
})().catch(()=>{console.error("IMPORT FAIL");process.exitCode=1;});
