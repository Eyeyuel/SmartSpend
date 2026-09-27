package com.eyuel.smartspend

import android.app.Application
import com.eyuel.smartspend.data.local.SmartSpendDatabase
import com.eyuel.smartspend.data.repository.TransactionRepository
import com.eyuel.smartspend.data.repository.TransactionRepositoryImpl

class SmartSpendApp : Application() {

    lateinit var database: SmartSpendDatabase
        private set

    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = SmartSpendDatabase.getDatabase(this)
        repository = TransactionRepositoryImpl(database.transactionDao())
    }

    companion object {
        lateinit var instance: SmartSpendApp
            private set
    }
}
