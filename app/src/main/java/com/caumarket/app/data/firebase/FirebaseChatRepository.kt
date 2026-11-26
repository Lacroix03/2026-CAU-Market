package com.caumarket.app.data.firebase

import android.net.Uri
import android.util.Base64
import com.caumarket.app.core.error.*
import com.caumarket.app.core.model.*
import com.caumarket.app.data.repository.ChatRepository
import com.caumarket.app.core.validation.Validation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

class FirebaseChatRepository(private val client: FirebaseClient) : ChatRepository {
 private val boundaries=ConcurrentHashMap<String,String>()
 private fun uid()=client.auth.currentUser?.uid ?: throw MarketException(MarketErrorCode.AUTH_REQUIRED)
 private fun <T> watch(uid: String,query: Query,decode: (List<DocumentSnapshot>)->T): Flow<Result<T>> = callbackFlow {
  val authListener=FirebaseAuth.AuthStateListener { auth ->
   if(auth.currentUser?.uid!=uid) {
    boundaries.keys.filter {it.startsWith(uid+":")}.forEach {boundaries.remove(it)}
    trySend(Result.failure(MarketException(MarketErrorCode.AUTH_REQUIRED)));close()
   }
  }
  client.auth.addAuthStateListener(authListener)
  val registration=query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot,error ->
   if(client.auth.currentUser?.uid!=uid) return@addSnapshotListener
   if(error!=null) {trySend(Result.failure(marketError(error)));close()}
   else if(snapshot!=null && !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
    try {trySend(Result.success(decode(snapshot.documents)))}
    catch(e: Exception) {trySend(Result.failure(marketError(e)));close()}
   }
  }
  awaitClose {registration.remove();client.auth.removeAuthStateListener(authListener)}
 }
 override fun observeChatRooms(): Flow<Result<List<ChatRoom>>> {
  val uid=client.auth.currentUser?.uid ?: return flowOf(Result.failure(MarketException(MarketErrorCode.AUTH_REQUIRED)))
  return watch(uid,client.firestore.collection("chatRooms").whereArrayContains("activeUserIds",uid)) { docs ->
   docs.map {Wire.room(it.data ?: error("Missing data"))}
    .sortedWith(compareByDescending<ChatRoom>{it.lastMessageAt?:it.createdAt}.thenBy {it.chatId})
  }
 }
 override fun observeMessages(chatId: String): Flow<Result<List<Message>>> {
  val uid=client.auth.currentUser?.uid ?: return flowOf(Result.failure(MarketException(MarketErrorCode.AUTH_REQUIRED)))
  return watch(uid,client.firestore.collection("chatRooms").document(chatId).collection("messages").orderBy("sequence")) { docs ->
   docs.map {Wire.message(it.data ?: error("Missing data"))}
  }.onEach { result ->
   // Advance only when the collector receives this snapshot, never in the producer's buffer.
   result.getOrNull()?.lastOrNull()?.let {boundaries[uid+":"+chatId]=it.messageId}
  }
 }
 override suspend fun getOrCreateChatRoom(productId: String): Result<String> = result {
  client.call("getOrCreateChatRoom",mapOf("productId" to productId)) as String
 }
 override suspend fun sendMessage(chatId: String,text: String,requestId: String?): Result<Unit> {
  val id=requestId?:client.firestore.collection("operations").document().id
  return result(id) {
   val uid=uid();val body=Validation.text(text)
   if(Validation.length(body) !in 1..1000) throw MarketException(MarketErrorCode.VALIDATION,"text")
   val payload=mapOf("requestId" to id,"chatId" to chatId,"text" to body)
   val saved=client.pending.getOrSave(uid,id,payload) {payload}
   client.call("sendMessage",saved);client.pending.complete(uid,id)
  }
 }
 override suspend fun sendImages(chatId: String,imageUris: List<Uri>,requestId: String?): Result<Unit> {
  val id=requestId?:client.firestore.collection("operations").document().id
  return result(id) {
   val uid=uid()
   if(imageUris.size !in 1..10) throw MarketException(MarketErrorCode.VALIDATION,"images")
   val signature=mapOf("kind" to "IMAGE_SEND","chatId" to chatId,"uris" to imageUris.map {it.toString()})
   val saved=client.pending.getOrSave(uid,id,signature) {
    val images=imageUris.map {client.encoder.encode(it)}
    mapOf("requestId" to id,"chatId" to chatId,"images" to images,"hashes" to images.map(ImageEncoder::hash))
   }
   @Suppress("UNCHECKED_CAST") val images=saved["images"] as List<String>
   client.transfer(saved-"images",images,"prepareImages","commitImages");client.pending.complete(uid,id)
  }
 }
 override suspend fun markAsRead(chatId: String): Result<Unit> = result {
  val message=boundaries[uid()+":"+chatId]?:throw MarketException(MarketErrorCode.CONFLICT,"throughMessageId")
  client.call("markAsRead",mapOf("chatId" to chatId,"throughMessageId" to message));Unit
 }
 override suspend fun markAsRead(chatId: String,throughMessageId: String): Result<Unit> = result {
  client.call("markAsRead",mapOf("chatId" to chatId,"throughMessageId" to throughMessageId));Unit
 }
 override suspend fun leaveChatRoom(chatId: String): Result<Unit> = result {
  val uid=uid();client.call("leaveChatRoom",mapOf("chatId" to chatId));boundaries.remove(uid+":"+chatId);Unit
 }
 override suspend fun getChatRoomInfo(chatId: String): Result<ChatRoomInfo> = result {
  val info=client.call("getChatRoomInfo",mapOf("chatId" to chatId)) as Map<*,*>
  @Suppress("UNCHECKED_CAST") val product=info["product"] as? Map<String,Any?>
  ChatRoomInfo(info["opponentId"] as String,info["opponentNickname"] as String,product?.let {client.product(it)})
 }
 override suspend fun getChatImage(chatId: String,messageId: String,index: Int): Result<ByteArray> = result {
  val image=client.call("getChatImage",mapOf("chatId" to chatId,"messageId" to messageId,"index" to index)) as Map<*,*>
  Base64.decode(image["base64"] as String,Base64.NO_WRAP)
 }
}
