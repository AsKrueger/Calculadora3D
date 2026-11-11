package com.tdcostmanager.app.core

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pago único «Quitar anuncios» (producto INAPP `remove_ads` en Play Console). Sin suscripciones.
 * Si el producto no existe o Google Play no está disponible, la opción simplemente no aparece.
 * La compra se recupera sola al reinstalar: se consulta a Google Play en cada arranque.
 */
object Billing {

    const val PRODUCT_ID = "remove_ads"
    private const val PREFS = "billing"
    private const val KEY_OWNED = "remove_ads_owned"

    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved

    private val _product = MutableStateFlow<ProductDetails?>(null)
    /** Detalles del producto (precio en la moneda del usuario); null = no se puede comprar ahora. */
    val product: StateFlow<ProductDetails?> = _product

    private var client: BillingClient? = null
    private lateinit var appContext: Context

    fun start(context: Context) {
        appContext = context.applicationContext
        // Arranque inmediato con lo último conocido; Google Play lo confirma después
        _adsRemoved.value = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_OWNED, false)
        if (client != null) return
        client = BillingClient.newBuilder(appContext)
            .setListener { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases.orEmpty()) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        connect()
    }

    private fun connect() {
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                queryProduct()
                client?.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                ) { r, purchases ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        val owned = purchases.any { it.isRemoveAds() && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        setOwned(owned)
                        handle(purchases)
                    }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client?.queryProductDetailsAsync(params) { r, list ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) _product.value = list.firstOrNull()
        }
    }

    fun buy(activity: Activity) {
        val details = _product.value ?: return
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
        ).build()
        client?.launchBillingFlow(activity, params)
    }

    private fun handle(purchases: List<Purchase>) {
        purchases.filter { it.isRemoveAds() && it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
            setOwned(true)
            // Sin reconocer en 3 días, Google devuelve el dinero
            if (!p.isAcknowledged) {
                client?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                ) {}
            }
        }
    }

    private fun setOwned(owned: Boolean) {
        _adsRemoved.value = owned
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_OWNED, owned).apply()
    }

    private fun Purchase.isRemoveAds() = products.contains(PRODUCT_ID)
}
