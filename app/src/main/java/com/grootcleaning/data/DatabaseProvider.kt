package com.grootcleaning.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: GrootDatabase? = null

    fun get(context: Context): GrootDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            GrootDatabase::class.java,
            "groot_cleaning.db"
        ).fallbackToDestructiveMigration().build().also { instance = it }
    }
}
