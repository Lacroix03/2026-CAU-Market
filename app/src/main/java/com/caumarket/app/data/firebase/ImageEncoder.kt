package com.caumarket.app.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.caumarket.app.core.error.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

internal class ImageEncoder(private val context: Context) {
 suspend fun encode(uri: Uri): String = withContext(Dispatchers.IO) {
  val resolver=context.contentResolver
  val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
  resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
  if(bounds.outWidth<=0 || bounds.outHeight<=0) throw MarketException(MarketErrorCode.VALIDATION,"image")
  var sample=1
  while(max(bounds.outWidth,bounds.outHeight)/sample>3200) sample*=2
  val orientation=try { resolver.openInputStream(uri).use { stream ->
   if(stream==null) 1 else ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)
  } } catch (_: java.io.IOException) { 1 }
  var bitmap=resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) }
   ?: throw MarketException(MarketErrorCode.VALIDATION,"image")
  try {
   val matrix=Matrix()
   when(orientation) {
    2 -> matrix.setScale(-1f,1f)
    3 -> matrix.setRotate(180f)
    4 -> matrix.setScale(1f,-1f)
    5 -> {matrix.setRotate(90f);matrix.postScale(-1f,1f)}
    6 -> matrix.setRotate(90f)
    7 -> {matrix.setRotate(-90f);matrix.postScale(-1f,1f)}
    8 -> matrix.setRotate(-90f)
   }
   if(!matrix.isIdentity) {
    val rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
    if(rotated!==bitmap) {bitmap.recycle();bitmap=rotated}
   }
   val longest=max(bitmap.width,bitmap.height)
   if(longest>1600) {
    val scale=1600.0/longest
    val resized=Bitmap.createScaledBitmap(bitmap,max(1,(bitmap.width*scale).roundToInt()),max(1,(bitmap.height*scale).roundToInt()),true)
    if(resized!==bitmap) {bitmap.recycle();bitmap=resized}
   }
   val output=ByteArrayOutputStream()
   if(!bitmap.compress(Bitmap.CompressFormat.JPEG,85,output)) throw MarketException(MarketErrorCode.VALIDATION,"image")
   val bytes=output.toByteArray()
   if(bytes.size>2*1024*1024) throw MarketException(MarketErrorCode.VALIDATION,"image")
   Base64.encodeToString(bytes,Base64.NO_WRAP)
  } finally {bitmap.recycle()}
 }
 companion object {
  fun hash(base64: String): String = MessageDigest.getInstance("SHA-256")
   .digest(Base64.decode(base64,Base64.NO_WRAP)).joinToString("") { "%02x".format(it.toInt() and 255) }
 }
}
