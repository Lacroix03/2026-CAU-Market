import {HttpsError} from "firebase-functions/v2/https";
export const fail = (code: string, field?: string): never => {
 const map: Record<string, any> = {VALIDATION:"invalid-argument",AUTH_REQUIRED:"unauthenticated",PERMISSION_DENIED:"permission-denied",NOT_FOUND:"not-found",CONFLICT:"aborted",INCOMPLETE_PROFILE:"failed-precondition"};
 throw new HttpsError(map[code] || "internal", code, {code,field:field??null});
};
export function text(value: unknown, field: string, max: number, min=0): string {
 if(typeof value!=="string") return fail("VALIDATION",field);
 const result=value.normalize("NFC").trim();
 if([...result].length<min || [...result].length>max) return fail("VALIDATION",field);
 return result;
}
export function id(value:unknown):string {
 if(typeof value!=="string" || !/^[A-Za-z0-9]{20}$/.test(value)) return fail("VALIDATION","id");
 return value;
}
export function schoolEmail(value: unknown):boolean {
 return typeof value==="string" && /^[^@\s]+@cau\.ac\.kr$/i.test(value);
}
export function choice(value:unknown, field:string, values:string[]):string {
 if(typeof value!=="string" || !values.includes(value)) return fail("VALIDATION",field);
 return value;
}
