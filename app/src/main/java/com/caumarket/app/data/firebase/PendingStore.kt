package com.caumarket.app.data.firebase

import android.content.Context
import com.caumarket.app.core.error.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

internal class PendingStore(context: Context) {
 private val directory=File(context.noBackupFilesDir,"market-pending").apply { mkdirs() }
 private val mutex=Mutex()
 private fun prefix(uid: String)=MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
  .joinToString("") { "%02x".format(it.toInt() and 255) }
 private fun file(uid: String,id: String): File {
  if(!Regex("^[A-Za-z0-9]{20}$").matches(id)) throw MarketException(MarketErrorCode.VALIDATION,"requestId")
  return File(directory,prefix(uid)+"-"+id+".json")
 }
 suspend fun getOrSave(uid: String,id: String,signature: Map<String,Any?>,make: suspend ()->Map<String,Any?>): Map<String,Any?> =
  mutex.withLock { withContext(Dispatchers.IO) {
   val file=file(uid,id)
   val signatureText=canonical(signature)
   if(file.exists()) {
    val saved=JSONObject(file.readText())
    if(saved.getString("signature")!=signatureText) throw MarketException(MarketErrorCode.CONFLICT,operationId=id)
    @Suppress("UNCHECKED_CAST")
    (unpack(saved.getJSONObject("payload")) as Map<String,Any?>)
   } else {
    val payload=make()
    val temp=File(file.path+".tmp")
    temp.writeText(JSONObject().put("signature",signatureText).put("payload",JSONObject(payload)).toString())
    if(!temp.renameTo(file)) {temp.delete();error("Pending operation save failed")}
    payload
   }
  } }
 suspend fun complete(uid: String,id: String) = mutex.withLock {withContext(Dispatchers.IO) {file(uid,id).delete();Unit}}
 suspend fun clear(uid: String) = mutex.withLock {withContext(Dispatchers.IO) {
  directory.listFiles()?.filter {it.name.startsWith(prefix(uid)+"-")}?.forEach {it.delete()};Unit
 }}
 private fun canonical(v: Any?): String = when(v) {
  null -> "null"
  is Map<*,*> -> v.entries.sortedBy {it.key as String}.joinToString(",","{","}") {JSONObject.quote(it.key as String)+":"+canonical(it.value)}
  is List<*> -> v.joinToString(",","[","]") {canonical(it)}
  is String -> JSONObject.quote(v)
  else -> v.toString()
 }
 private fun unpack(v: Any?): Any? = when(v) {
  JSONObject.NULL -> null
  is JSONObject -> v.keys().asSequence().associateWith {unpack(v.get(it))}
  is JSONArray -> (0 until v.length()).map {unpack(v.get(it))}
  else -> v
 }
}
