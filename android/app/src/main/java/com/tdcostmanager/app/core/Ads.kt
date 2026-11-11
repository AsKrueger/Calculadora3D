package com.tdcostmanager.app.core

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.tdcostmanager.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Publicidad para cubrir el mantenimiento. Pide el consentimiento (obligatorio en la UE) antes
 * de cargar nada; si el usuario no lo da o no hay conexión, simplemente no hay anuncios.
 */
object Ads {

    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** En la UE hay que ofrecer en Ajustes una forma de cambiar el consentimiento. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    private lateinit var consent: ConsentInformation

    fun start(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED) return
        consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyState()
                    initIfAllowed(activity)
                }
            },
            { initIfAllowed(activity) }
        )
        // Consentimiento de una sesión anterior: no hace falta esperar
        initIfAllowed(activity)
    }

    private fun updatePrivacyState() {
        _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initIfAllowed(context: Context) {
        if (!consent.canRequestAds()) return
        updatePrivacyState()
        if (started.getAndSet(true)) return
        MobileAds.initialize(context.applicationContext) { _ready.value = true }
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { updatePrivacyState() }
    }
}

/**
 * Barra de anuncio en la parte superior. No ocupa espacio hasta que hay un anuncio cargado:
 * si no hay anuncio (sin conexión, sin consentimiento o sin inventario), la barra no existe.
 */
@Composable
fun AdBar(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsState()
    val removed by Billing.adsRemoved.collectAsState()
    if (!ready || removed || !BuildConfig.ADS_ENABLED) return
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        val adView = remember(width) {
            AdView(context).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() { loaded = true }
                    override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
                }
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }

        // El AdView vive siempre (si no, no cargaría), pero con altura 0 hasta tener anuncio
        Box(Modifier.fillMaxWidth().then(if (loaded) Modifier else Modifier.height(0.dp))) {
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (loaded) HorizontalDivider()
}
