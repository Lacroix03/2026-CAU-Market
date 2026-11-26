package com.caumarket.app

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.caumarket.app.di.AppContainer
import com.caumarket.app.core.model.*
import com.caumarket.app.core.error.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

/** Requires the local demo Emulator Suite. Does not use or create cloud resources. */
class BackendIntegrationTest {
 @Test fun repositoriesSupportProductChatAndAuthenticatedImages() = runBlocking {
  withTimeout(180000) {
   val context=InstrumentationRegistry.getInstrumentation().targetContext
   if (android.os.Build.VERSION.SDK_INT >= 37) {
    InstrumentationRegistry.getInstrumentation().uiAutomation
     .grantRuntimePermission(context.packageName,"android.permission.ACCESS_LOCAL_NETWORK")
   }
   val container=AppContainer(context)
   val auth=container.authRepository
   val products=container.productRepository
   val chats=container.chatRepository
   auth.logout().getOrThrow()
   assertEquals(MarketErrorCode.AUTH_REQUIRED,
    (chats.getOrCreateChatRoom("abcdefghijklmnopqrst").exceptionOrNull() as MarketException).code)
   val suffix=UUID.randomUUID().toString().replace("-","")
   val seller="seller$suffix@cau.ac.kr"
   val buyer="buyer$suffix@cau.ac.kr"
   val password="DemoOnly123!"
   val file=File(context.cacheDir,"fixture-$suffix.jpg")
   val bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888)
   file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.JPEG,85,it)}
   bitmap.recycle()
   try {
    auth.signUp(seller,password,"판매자").getOrThrow()
    assertNull(auth.getCurrentUser().getOrThrow())
    auth.login(seller,password).getOrThrow()
    val input=ProductInput("테스트 교재",null,0,"TEXTBOOK","GOOD","LIBRARY",null,null)
    val productId=products.createProduct(input,listOf(Uri.fromFile(file),Uri.fromFile(file))).getOrThrow()
    val original=products.getProduct(productId).getOrThrow()
    assertEquals(2,original.imageUrls.size)
    products.updateProduct(productId,ProductUpdate(input.copy(title="수정 교재"),original.updatedAt),
     listOf(ProductPhoto.Existing(original.imageUrls[1]))).getOrThrow()
    assertEquals(original.imageUrls[1],products.getProduct(productId).getOrThrow().imageUrls.first())
    products.changeProductStatus(productId,"SOLD").getOrThrow()
    assertFalse(products.getProducts().getOrThrow().any {it.productId==productId})
    assertTrue(products.getProducts(query="교재").getOrThrow().any {it.productId==productId})
    products.changeProductStatus(productId,"RESERVED").getOrThrow()
    auth.logout().getOrThrow()
    auth.signUp(buyer,password,"구매자").getOrThrow()
    val buyerUser=auth.login(buyer,password).getOrThrow()
    val chatId=chats.getOrCreateChatRoom(productId).getOrThrow()
    chats.sendMessage(chatId,"문의합니다").getOrThrow()
    chats.sendImages(chatId,listOf(Uri.fromFile(file))).getOrThrow()
    val received=chats.observeMessages(chatId).first {it.isFailure || it.getOrThrow().size==2}.getOrThrow()
    val photo=received.first {it.type=="IMAGE"}
    assertTrue(photo.imageUrls.first().startsWith("gs://"))
    auth.logout().getOrThrow()
    val sellerUser=auth.login(seller,password).getOrThrow()
    val unread=chats.observeChatRooms().first {it.isFailure || it.getOrThrow().any {r->r.chatId==chatId}}.getOrThrow().first {it.chatId==chatId}
    assertEquals(2,unread.unreadCount[sellerUser.userId])
    val messages=chats.observeMessages(chatId).first {it.isFailure || it.getOrThrow().size==2}.getOrThrow()
    chats.markAsRead(chatId).getOrThrow()
    chats.markAsRead(chatId,messages.last().messageId).getOrThrow()
    val read=chats.observeChatRooms().first {it.isFailure || it.getOrThrow().any {r->r.chatId==chatId && r.unreadCount[sellerUser.userId]==0}}.getOrThrow()
    assertEquals(0,read.first {it.chatId==chatId}.unreadCount[sellerUser.userId])
    assertTrue(chats.getChatImage(chatId,photo.messageId,0).getOrThrow().isNotEmpty())
    assertEquals(buyerUser.userId,chats.getChatRoomInfo(chatId).getOrThrow().opponentId)
    auth.logout().getOrThrow()
    auth.login(buyer,password).getOrThrow()
    chats.leaveChatRoom(chatId).getOrThrow()
    val forbidden=chats.getChatImage(chatId,photo.messageId,0).exceptionOrNull() as MarketException
    assertEquals(MarketErrorCode.PERMISSION_DENIED,forbidden.code)
    auth.logout().getOrThrow()
    auth.login(seller,password).getOrThrow()
    assertTrue(chats.getChatImage(chatId,photo.messageId,0).getOrThrow().isNotEmpty())
    products.deleteProduct(productId).getOrThrow()
    assertNull(chats.getChatRoomInfo(chatId).getOrThrow().product)
    auth.logout().getOrThrow()
   } finally {file.delete()}
  }
 }
}
