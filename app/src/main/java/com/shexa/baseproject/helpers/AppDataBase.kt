package com.shexa.baseproject.helpers

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shexa.baseproject.daos.ReadHistoryDao
import com.shexa.baseproject.daos.WriteHistoryDao
import com.shexa.baseproject.entities.ReadHistoryModel
import com.shexa.baseproject.entities.WriteHistoryModel

@Database(entities = [WriteHistoryModel::class,ReadHistoryModel::class], version = 1)
abstract class AppDataBase : RoomDatabase()
{
    abstract fun WriteHistoryDao() : WriteHistoryDao
    abstract fun ReadHistoryDao() : ReadHistoryDao

    companion object{

        private var INSTANCE : AppDataBase?=null

        @Synchronized
        fun getInstance(context: Context) : AppDataBase
        {
            if(INSTANCE == null)
            {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                                                AppDataBase::class.java,
                                           "history").build()
            }
            return INSTANCE!!
        }
    }
}