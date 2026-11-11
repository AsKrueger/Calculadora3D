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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tdcostmanager.app.core.Ads
import com.tdcostmanager.app.core.Billing
import com.tdcostmanager.app.data.settings.ThemeMode
import com.tdcostmanager.app.ui.AppRoot
import com.tdcostmanager.app.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_PREFIX = "com.tdcostmanager.app.shortcut."
        const val SHORTCUT_NEW_CALC = "new_calc"
        const val SHORTCUT_IMPORT = "import"
        const val SHORTCUT_NEW_QUOTE = "new_quote"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)
        publishShortcuts()
        com.tdcostmanager.app.core.PrintsWidget.refresh(this)

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

    /** Accesos directos al mantener pulsado el icono, en el idioma actual. */
    private fun publishShortcuts() {
        fun shortcut(id: String, label: Int, icon: Int) = ShortcutInfoCompat.Builder(this, id)
            .setShortLabel(getString(label))
            .setIcon(IconCompat.createWithResource(this, icon))
            .setIntent(Intent(this, MainActivity::class.java).setAction(ACTION_PREFIX + id))
            .build()
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(
                this,
                listOf(
                    shortcut(SHORTCUT_NEW_CALC, R.string.shortcut_new_calc, R.drawable.ic_shortcut_calc),
                    shortcut(SHORTCUT_IMPORT, R.string.shortcut_import, R.drawable.ic_shortcut_import),
                    shortcut(SHORTCUT_NEW_QUOTE, R.string.shortcut_new_quote, R.drawable.ic_shortcut_quote)
                )
            )
        }
    }

    /** Un G-code o 3MF abierto o compartido con la app, o un acceso directo. */
    private fun handleIntent(intent: Intent?) {
        intent?.action?.takeIf { it.startsWith(ACTION_PREFIX) }?.let {
            container.pendingShortcut.value = it.removePrefix(ACTION_PREFIX)
            return
        }
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
