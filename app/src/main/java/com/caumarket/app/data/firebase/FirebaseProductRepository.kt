package com.caumarket.app.data.firebase

import android.net.Uri
import com.caumarket.app.core.error.*
import com.caumarket.app.core.model.*
import com.caumarket.app.data.repository.ProductRepository
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class FirebaseProductRepository(private val client: FirebaseClient) : ProductRepository {
 private fun uid()=client.auth.currentUser?.uid ?: throw MarketException(MarketErrorCode.AUTH_REQUIRED)
 override suspend fun getProducts(query: String?,category: String?,includeSold: Boolean): Result<List<Product>> = result {
  uid()
  val term=com.caumarket.app.core.validation.Validation.text(query.orEmpty())
  if(category!=null && category !in listOf("TEXTBOOK","LIVING","ELECTRONICS","CLOTHING","ETC")) throw MarketException(MarketErrorCode.VALIDATION,"category")
  client.firestore.collection("products").orderBy("createdAt",Query.Direction.DESCENDING).get(Source.SERVER).await()
   .documents.map {client.product(it.data ?: error("Missing data"))}.filter {
    (category==null||it.category==category) && (includeSold||it.status!="SOLD") &&
     (term.isEmpty()||it.title.contains(term,true)||it.description?.contains(term,true)==true)
   }
 }
 override suspend fun getMyProducts(status: String?): Result<List<Product>> = result {
  if(status!=null && status !in listOf("SELLING","RESERVED","SOLD")) throw MarketException(MarketErrorCode.VALIDATION,"status")
  client.firestore.collection("products").whereEqualTo("sellerId",uid()).orderBy("createdAt",Query.Direction.DESCENDING)
   .get(Source.SERVER).await().documents.map {client.product(it.data ?: error("Missing data"))}
   .filter {status==null||it.status==status}
 }
 override suspend fun getProduct(productId: String): Result<Product> = result {
  uid();val document=client.firestore.collection("products").document(productId).get(Source.SERVER).await()
  client.product(document.data ?: throw MarketException(MarketErrorCode.NOT_FOUND))
 }
 override suspend fun createProduct(input: ProductInput,imageUris: List<Uri>,requestId: String?): Result<String> {
  val id=requestId?:client.firestore.collection("operations").document().id
  return result(id) {
   val uid=uid()
   if(imageUris.size !in 1..15) throw MarketException(MarketErrorCode.VALIDATION,"images")
   val signature=mapOf("kind" to "PRODUCT_CREATE","input" to Wire.input(input),"uris" to imageUris.map {it.toString()})
   val saved=client.pending.getOrSave(uid,id,signature) {
    val images=imageUris.map {client.encoder.encode(it)}
    mapOf("requestId" to id,"input" to Wire.input(input),"existing" to emptyList<String>(),
     "hashes" to images.map(ImageEncoder::hash),"images" to images)
   }
   @Suppress("UNCHECKED_CAST") val images=saved["images"] as List<String>
   val productId=client.transfer(saved-"images",images,"prepareProduct","commitProduct") as String
   client.pending.complete(uid,id);productId
  }
 }
 override suspend fun updateProduct(productId: String,update: ProductUpdate,photos: List<ProductPhoto>,requestId: String?): Result<Unit> {
  val id=requestId?:client.firestore.collection("operations").document().id
  return result(id) {
   val uid=uid()
   if(photos.size !in 1..15 || photos.dropWhile {it is ProductPhoto.Existing}.any {it is ProductPhoto.Existing})
    throw MarketException(MarketErrorCode.VALIDATION,"images")
   val existing=photos.filterIsInstance<ProductPhoto.Existing>().map {it.url}
   val uris=photos.filterIsInstance<ProductPhoto.New>().map {it.uri}
   val signature=mapOf("kind" to "PRODUCT_UPDATE","productId" to productId,"input" to Wire.input(update.input),
    "expectedUpdatedAt" to Wire.time(update.expectedUpdatedAt),"existing" to existing,"uris" to uris.map {it.toString()})
   val saved=client.pending.getOrSave(uid,id,signature) {
    val images=uris.map {client.encoder.encode(it)}
    mapOf("requestId" to id,"productId" to productId,"input" to Wire.input(update.input),
     "expectedUpdatedAt" to Wire.time(update.expectedUpdatedAt),"existing" to existing,
     "hashes" to images.map(ImageEncoder::hash),"images" to images)
   }
   @Suppress("UNCHECKED_CAST") val images=saved["images"] as List<String>
   client.transfer(saved-"images",images,"prepareProduct","commitProduct")
   client.pending.complete(uid,id)
  }
 }
 override suspend fun deleteProduct(productId: String): Result<Unit> = result {
  client.call("deleteProduct",mapOf("productId" to productId));Unit
 }
 override suspend fun changeProductStatus(productId: String,status: String): Result<Unit> = result {
  client.call("changeProductStatus",mapOf("productId" to productId,"status" to status));Unit
 }
}
