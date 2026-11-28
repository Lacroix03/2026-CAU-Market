package com.caumarket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.caumarket.app.ui.navigation.CauMarketApp
import com.caumarket.app.ui.theme.CAUMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CauMarketApplication).container
        setContent {
            CAUMarketTheme {
                CauMarketApp(container = container)
            }
        }
    }
}
