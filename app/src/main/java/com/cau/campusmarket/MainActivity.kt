package com.cau.campusmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cau.campusmarket.ui.navigation.CauMarketApp
import com.cau.campusmarket.ui.theme.CauCampusMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CauMarketApplication).container
        setContent {
            CauCampusMarketTheme {
                CauMarketApp(container = container)
            }
        }
    }
}
