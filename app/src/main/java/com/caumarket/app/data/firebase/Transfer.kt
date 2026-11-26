package com.caumarket.app.data.firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import com.caumarket.app.core.error.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Firebase callable wire protocol, restricted to the configured local demo endpoint. */
internal suspend fun FirebaseClient.call(name: String,data: Map<String,Any?>): Any? {
 require(Regex("[A-Za-z][A-Za-z0-9]*").matches(name))
 val user=auth.currentUser
 val token=user?.getIdToken(false)?.await()?.token
 if(auth.currentUser?.uid!=user?.uid) throw MarketException(MarketErrorCode.AUTH_REQUIRED)
 return withContext(Dispatchers.IO) {
  ensureActive()
  val connection=URL("http://$host:5001/demo-cau-market/asia-northeast3/$name").openConnection() as HttpURLConnection
  try {
   connection.requestMethod="POST"
   connection.instanceFollowRedirects=false
   connection.useCaches=false
   connection.connectTimeout=30000
   connection.readTimeout=60000
   connection.doOutput=true
   connection.setRequestProperty("Content-Type","application/json; charset=utf-8")
   if(token!=null) connection.setRequestProperty("Authorization","Bearer $token")
   val bytes=JSONObject().put("data",JSONObject(data)).toString().toByteArray(Charsets.UTF_8)
   connection.setFixedLengthStreamingMode(bytes.size)
   connection.outputStream.use {it.write(bytes)}
   val status=connection.responseCode
   val stream=if(status in 200..299) connection.inputStream else connection.errorStream
   val body=stream?.bufferedReader(Charsets.UTF_8)?.use {it.readText()}.orEmpty()
   ensureActive()
   if(auth.currentUser?.uid!=user?.uid) throw MarketException(MarketErrorCode.AUTH_REQUIRED)
   val response=try {JSONObject(body)} catch (_: org.json.JSONException) {
    throw MarketException(if(status>=500) MarketErrorCode.NETWORK else MarketErrorCode.UNKNOWN,retryable=status>=500)
   }
   val error=response.optJSONObject("error")
   if(error!=null || status !in 200..299) {
    val details=error?.optJSONObject("details")
    val code=MarketErrorCode.entries.find {it.name==details?.optString("code")} ?: when(error?.optString("status")) {
     "UNAUTHENTICATED" -> MarketErrorCode.AUTH_REQUIRED
     "PERMISSION_DENIED" -> MarketErrorCode.PERMISSION_DENIED
     "INVALID_ARGUMENT" -> MarketErrorCode.VALIDATION
     "NOT_FOUND" -> MarketErrorCode.NOT_FOUND
     "ABORTED","ALREADY_EXISTS","FAILED_PRECONDITION" -> MarketErrorCode.CONFLICT
     "UNAVAILABLE","DEADLINE_EXCEEDED" -> MarketErrorCode.NETWORK
     "RESOURCE_EXHAUSTED" -> MarketErrorCode.QUOTA_EXCEEDED
     else -> MarketErrorCode.UNKNOWN
    }
    val field=details?.opt("field") as? String
    throw MarketException(code,field,retryable=code==MarketErrorCode.NETWORK||code==MarketErrorCode.QUOTA_EXCEEDED)
   }
   if(!response.has("result") && !response.has("data")) throw MarketException(MarketErrorCode.UNKNOWN)
   decodeCallable(if(response.has("result")) response.get("result") else response.get("data"))
  } finally {connection.disconnect()}
 }
}
private fun decodeCallable(value: Any?): Any? = when(value) {
 JSONObject.NULL -> null
 is JSONObject -> if(value.optString("@type")=="type.googleapis.com/google.protobuf.Int64Value") value.getString("value").toLong()
  else value.keys().asSequence().associateWith {decodeCallable(value.get(it))}
 is JSONArray -> (0 until value.length()).map {decodeCallable(value.get(it))}
 else -> value
}
internal suspend fun FirebaseClient.transfer(payload: Map<String,Any?>, images: List<String>, prepare: String, commit: String): Any? {
 val state=call(prepare,payload) as Map<*,*>
 if(state["state"]=="PREPARED") for((index,image) in images.withIndex())
  call("uploadImage",mapOf("requestId" to payload["requestId"],"index" to index,"base64" to image))
 return call(commit,payload)
}
