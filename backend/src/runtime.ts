import {initializeApp} from "firebase-admin/app";
import {getFirestore,FieldValue} from "firebase-admin/firestore";
import {onCall,HttpsError} from "firebase-functions/v2/https";
import {fail,schoolEmail} from "./validation";
initializeApp({projectId:"demo-cau-market",storageBucket:"demo-cau-market.appspot.com"});
export const db=getFirestore();
export const stamp=()=>FieldValue.serverTimestamp();
export function localOnly() {
 if(!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST)
  throw new HttpsError("failed-precondition","Emulator configuration required");
}
export const callable=(handler:(uid:string,data:any)=>Promise<any>,profile=true)=>onCall({region:"asia-northeast3"},async request=>{
 localOnly();
 if(!request.auth || !schoolEmail(request.auth.token.email)) return fail("AUTH_REQUIRED");
 if(profile && !(await db.doc("users/"+request.auth.uid).get()).exists) return fail("INCOMPLETE_PROFILE");
 try { return await handler(request.auth.uid,request.data??{}); }
 catch(error) {
  if(error instanceof HttpsError) throw error;
  // Do not let SDK exceptions leak paths, tokens, or raw request data into platform logs.
  throw new HttpsError("internal","UNKNOWN",{code:"UNKNOWN",field:null});
 }
});
export function participating(room:any,uid:string,both=false) {
 if(!room || !room.activeUserIds.includes(uid) || (both && room.activeUserIds.length!==2)) fail("PERMISSION_DENIED");
}
