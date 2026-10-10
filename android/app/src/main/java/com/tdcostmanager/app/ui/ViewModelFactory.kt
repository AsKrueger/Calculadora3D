package com.tdcostmanager.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tdcostmanager.app.data.local.TokenManager
import com.tdcostmanager.app.data.repository.AuthRepository
import com.tdcostmanager.app.data.repository.HealthRepository
import com.tdcostmanager.app.data.repository.MachineRepository
import com.tdcostmanager.app.data.repository.MaterialRepository
import com.tdcostmanager.app.data.repository.ProjectRepository
import com.tdcostmanager.app.data.repository.QuoteRepository
import com.tdcostmanager.app.data.repository.ToolRepository
import com.tdcostmanager.app.ui.auth.AuthViewModel
import com.tdcostmanager.app.ui.health.HealthViewModel
import com.tdcostmanager.app.ui.machine.MachineViewModel
import com.tdcostmanager.app.ui.material.MaterialViewModel
import com.tdcostmanager.app.ui.project.ProjectViewModel
import com.tdcostmanager.app.ui.quote.QuoteViewModel
import com.tdcostmanager.app.ui.tool.ToolViewModel

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                val tokenManager = TokenManager(context)
                val repository = AuthRepository(tokenManager = tokenManager)
                AuthViewModel(repository) as T
            }
            modelClass.isAssignableFrom(HealthViewModel::class.java) -> {
                HealthViewModel(HealthRepository()) as T
            }
            modelClass.isAssignableFrom(ProjectViewModel::class.java) -> {
                ProjectViewModel(ProjectRepository()) as T
            }
            modelClass.isAssignableFrom(MaterialViewModel::class.java) -> {
                MaterialViewModel(MaterialRepository()) as T
            }
            modelClass.isAssignableFrom(MachineViewModel::class.java) -> {
                MachineViewModel(MachineRepository()) as T
            }
            modelClass.isAssignableFrom(ToolViewModel::class.java) -> {
                ToolViewModel(ToolRepository()) as T
            }
            modelClass.isAssignableFrom(QuoteViewModel::class.java) -> {
                QuoteViewModel(QuoteRepository()) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
