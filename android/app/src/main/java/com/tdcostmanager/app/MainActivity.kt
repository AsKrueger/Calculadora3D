package com.tdcostmanager.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tdcostmanager.app.core.Ads
import com.tdcostmanager.app.core.Billing
import com.tdcostmanager.app.data.settings.ThemeMode
import com.tdcostmanager.app.ui.AppRoot
import com.tdcostmanager.app.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            val settings by container.settings.settings.collectAsState(initial = null)
            // Los ajustes tardan milisegundos: mientras, solo el fondo de la ventana
            val s = settings ?: return@setContent
            val dark = when (s.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AppTheme(darkTheme = dark) {
                AppRoot(settings = s)
            }
        }

        Billing.start(this)
        Ads.start(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Un G-code o 3MF abierto o compartido con la app. */
    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) container.pendingImport.value = uri
    }
}
