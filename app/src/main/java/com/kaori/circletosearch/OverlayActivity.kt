package com.kaori.circletosearch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kaori.circletosearch.data.BitmapRepository
import com.kaori.circletosearch.ui.CircleToSearchScreen
import com.kaori.circletosearch.ui.theme.CircleToSearchTheme
import com.kaori.circletosearch.utils.StorageUtils

class OverlayActivity : ComponentActivity() {
    private val screenshotBitmap = mutableStateOf<Bitmap?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrapBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        loadScreenshot()

        setContent {
            CircleToSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    CircleToSearchScreen(
                        screenshot = screenshotBitmap.value,
                        onClose = {
                            BitmapRepository.clear()
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        screenshotBitmap.value = null
        loadScreenshot()
    }

    private fun loadScreenshot() {
        screenshotBitmap.value = BitmapRepository.getScreenshot()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            BitmapRepository.clear()
            StorageUtils.clearAppCache(this)
        }
    }
}
