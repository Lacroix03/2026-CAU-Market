package com.caumarket.app.data.firebase

import com.caumarket.app.core.model.*
import com.google.firebase.Timestamp

internal object Wire {
 fun timestamp(value: Any?): Timestamp = when(value) {
  is Timestamp -> value
  is Map<*, *> -> Timestamp(((value["seconds"] ?: value["_seconds"]) as Number).toLong(),
   ((value["nanoseconds"] ?: value["_nanoseconds"]) as Number).toInt())
  else -> error("Invalid timestamp")
 }
 fun time(value: Timestamp?): Map<String, Any>? = value?.let { mapOf("seconds" to it.seconds,"nanoseconds" to it.nanoseconds) }
 fun product(m: Map<String,Any?>): Product = Product(
  m.string("productId"),m.string("sellerId"),m.string("sellerNickname"),m.string("title"),
  m["description"] as String?,m.number("price").toInt(),m.string("category"),m.string("condition"),
  m.strings("imageUrls"),m.string("place"),m["placeDetail"] as String?,
  m["disposeBy"]?.let(::timestamp),m.string("status"),timestamp(m["createdAt"]),timestamp(m["updatedAt"]))
 fun room(m: Map<String,Any?>): ChatRoom = ChatRoom(m.string("chatId"),m.string("productId"),
  m.string("sellerId"),m.string("buyerId"),m.strings("activeUserIds"),m["lastMessage"] as String?,
  m["lastMessageAt"]?.let(::timestamp),(m["unreadCount"] as Map<*,*>).entries.associate { it.key as String to (it.value as Number).toInt() },
  (m["lastReadAt"] as Map<*,*>).entries.associate { it.key as String to timestamp(it.value) },
  timestamp(m["createdAt"]),m.number("lastSequence").toLong(),
  (m["lastReadSequence"] as Map<*,*>).entries.associate { it.key as String to (it.value as Number).toLong() })
 fun message(m: Map<String,Any?>): Message = Message(m.string("messageId"),m.string("senderId"),
  m.string("type"),m["text"] as String?,m.strings("imageUrls"),timestamp(m["createdAt"]),m.number("sequence").toLong())
 fun input(v: ProductInput): Map<String,Any?> = linkedMapOf("title" to v.title,"description" to v.description,
  "price" to v.price,"category" to v.category,"condition" to v.condition,"place" to v.place,
  "placeDetail" to v.placeDetail,"disposeBy" to time(v.disposeBy))
 private fun Map<String,Any?>.string(k: String) = this[k] as String
 private fun Map<String,Any?>.number(k: String) = this[k] as Number
 private fun Map<String,Any?>.strings(k: String) = (this[k] as List<*>).map { it as String }
}
