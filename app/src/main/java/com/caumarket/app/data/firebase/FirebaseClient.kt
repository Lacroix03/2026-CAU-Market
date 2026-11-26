package com.caumarket.app.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/** Local demo only. Frontend creates one instance and shares it between repositories. */
class FirebaseClient(context: Context, internal val host: String = "10.0.2.2") {
    internal val pending=PendingStore(context.applicationContext)
    internal val encoder=ImageEncoder(context.applicationContext)
    internal fun product(data: Map<String,Any?>): com.caumarket.app.core.model.Product =
        Wire.product(data).let { product ->
            product.copy(imageUrls=product.imageUrls.map { url ->
                val uri=android.net.Uri.parse(url)
                if(uri.host=="127.0.0.1" || uri.host=="localhost")
                    uri.buildUpon().encodedAuthority(host+":9199").build().toString() else url
            })
        }
    init { require(Regex("[A-Za-z0-9.-]+").matches(host)) }
    private val app = FirebaseApp.initializeApp(context.applicationContext,
        FirebaseOptions.Builder().setProjectId("demo-cau-market")
            .setApplicationId("1:123456789:android:caumarketdemo").setApiKey("demo-key")
            .setStorageBucket("demo-cau-market.appspot.com").build(), "cau-market-local")
    val auth: FirebaseAuth = FirebaseAuth.getInstance(app).apply { useEmulator(host, 9099) }
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(app).apply { useEmulator(host, 8080) }
    val storage: FirebaseStorage = FirebaseStorage.getInstance(app).apply { useEmulator(host, 9199) }
}
