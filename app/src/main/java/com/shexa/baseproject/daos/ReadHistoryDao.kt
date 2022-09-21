package com.shexa.baseproject.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shexa.baseproject.entities.ReadHistoryModel
import com.shexa.baseproject.entities.WriteHistoryModel

@Dao
interface ReadHistoryDao
{
    @Query("select * from readHistory")
    fun getAllReadRecord() : List<ReadHistoryModel>

    @Insert
    fun insertReadRecord(readModel : ReadHistoryModel)
}