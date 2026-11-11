package com.tdcostmanager.app

import android.app.Application
import android.content.Context
import android.net.Uri
import com.tdcostmanager.app.core.BackupManager
import com.tdcostmanager.app.core.ImageStore
import com.tdcostmanager.app.data.Repository
import com.tdcostmanager.app.data.Seeder
import com.tdcostmanager.app.data.db.AppDatabase
import com.tdcostmanager.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Dependencias de la app, creadas una sola vez. */
class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.build(context)
    val settings = SettingsRepository(context)
    val images = ImageStore(context)
    val repository = Repository(db, settings, images)
    val backup = BackupManager(context, db, settings, images)
    val seeder = Seeder(context, db)

    /** Archivo del laminador compartido con la app, pendiente de abrir en la calculadora. */
    val pendingImport = MutableStateFlow<Uri?>(null)

    /** Acción pedida desde un acceso directo del icono, pendiente de atender. */
    val pendingShortcut = MutableStateFlow<String?>(null)
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.tdcostmanager.app.core.PrintTimer.init(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as App).container
