package com.tdcostmanager.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.InvalidBackupException
import com.tdcostmanager.app.core.PvpcClient
import com.tdcostmanager.app.core.PvpcDay
import com.tdcostmanager.app.data.PrinterTemplate
import com.tdcostmanager.app.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

data class Storage(val imagesBytes: Long = 0, val cacheBytes: Long = 0)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    val settings: StateFlow<AppSettings?> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val printers = c.db.printers().observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val channels = c.db.channels().observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _storage = MutableStateFlow(Storage())
    val storage: StateFlow<Storage> = _storage

    private val _pvpc = MutableStateFlow<PvpcDay?>(null)
    val pvpc: StateFlow<PvpcDay?> = _pvpc
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init { refreshStorage() }

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { c.settings.update(transform) }

    fun refreshStorage() = viewModelScope.launch {
        _storage.value = withContext(Dispatchers.IO) { Storage(c.images.imagesBytes(), c.images.cacheBytes()) }
    }

    /** Idioma de la app; vacío = el del sistema. */
    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    fun currentLanguage(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    fun loadPvpc(apply: Boolean) = viewModelScope.launch {
        _busy.value = true
        runCatching { PvpcClient.today() }
            .onSuccess { day ->
                _pvpc.value = day
                if (apply) c.settings.update { it.copy(energyPrice = day.average.setScale(4, RoundingMode.HALF_UP)) }
            }
            .onFailure { messages.send(getApplication<Application>().getString(R.string.pvpc_failed)) }
        _busy.value = false
    }

    /** Al activarlo, copia a la app las fotos de la galería para que no se pierdan. */
    fun setSaveImagesOffline(enabled: Boolean) = viewModelScope.launch {
        c.settings.update { it.copy(saveImagesOffline = enabled) }
        if (enabled) {
            _busy.value = true
            var copied = 0
            c.db.pieces().images().distinct().filter { it.startsWith("content:") }.forEach { ref ->
                c.images.localize(ref)?.let { local ->
                    c.db.pieces().replaceImage(ref, local)
                    copied++
                }
            }
            _busy.value = false
            if (copied > 0) messages.send(getApplication<Application>().resources.getQuantityString(R.plurals.images_copied, copied, copied))
        }
        refreshStorage()
    }

    fun clearCache() = viewModelScope.launch {
        c.images.clearCache()
        val inUse = c.db.pieces().images().toSet() + setOfNotNull(c.settings.current().business.logo)
        c.images.purgeUnused(inUse)
        refreshStorage()
        messages.send(getApplication<Application>().getString(R.string.settings_cache_cleared))
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val ok = runCatching { c.backup.export(uri) }.isSuccess
        _busy.value = false
        messages.send(getApplication<Application>().getString(if (ok) R.string.backup_done else R.string.backup_failed))
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        _busy.value = true
        val result = runCatching { c.backup.restore(uri) }
        _busy.value = false
        c.settings.saveDraft(null)
        val app = getApplication<Application>()
        messages.send(
            when {
                result.isSuccess -> app.getString(R.string.restore_done)
                result.exceptionOrNull() is InvalidBackupException -> app.getString(R.string.restore_invalid)
                else -> app.getString(R.string.restore_failed)
            }
        )
        refreshStorage()
    }

    /** Borra todo y vuelve a la bienvenida, como recién instalada. */
    fun clearAllData() = viewModelScope.launch {
        withContext(Dispatchers.IO) { c.db.clearAllTables() }
        c.images.clearAll()
        c.images.clearCache()
        c.settings.clear()
        refreshStorage()
    }

    fun setLogo(uri: Uri) = viewModelScope.launch {
        val ref = c.images.importFromUri(uri, offline = true) ?: return@launch
        val old = c.settings.current().business.logo
        c.settings.update { it.copy(business = it.business.copy(logo = ref)) }
        c.images.delete(old)
    }

    fun removeLogo() = viewModelScope.launch {
        val old = c.settings.current().business.logo
        c.settings.update { it.copy(business = it.business.copy(logo = null)) }
        c.images.delete(old)
    }

    /** Primera configuración: catálogo inicial y valores elegidos en la bienvenida. */
    fun finishOnboarding(
        printer: PrinterTemplate?,
        currency: String,
        energy: BigDecimal?,
        labor: BigDecimal?,
        vat: BigDecimal?
    ) = viewModelScope.launch {
        if (!c.settings.current().seeded) c.seeder.seed(printer)
        val firstPrinter = c.db.printers().all().firstOrNull()?.id
        val firstChannel = c.db.channels().all().firstOrNull()?.id
        c.settings.update {
            it.copy(
                currency = currency,
                energyPrice = energy ?: it.energyPrice,
                laborRate = labor ?: it.laborRate,
                vatPercent = vat ?: it.vatPercent,
                defaultPrinterId = firstPrinter,
                defaultChannelId = firstChannel,
                seeded = true,
                onboardingDone = true
            )
        }
    }
}
