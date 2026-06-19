package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.NabloosRepository

class NabloosApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        NabloosRepository(
            chandelierDao = database.chandelierDao(),
            articleDao = database.articleDao(),
            customDesignRequestDao = database.customDesignRequestDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
