package com.gearhead.redline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.gearhead.redline.ui.navigation.RedlineNavHost
import com.gearhead.redline.ui.theme.Ink
import com.gearhead.redline.ui.theme.RedlineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedlineTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    RedlineNavHost()
                }
            }
        }
    }
}
