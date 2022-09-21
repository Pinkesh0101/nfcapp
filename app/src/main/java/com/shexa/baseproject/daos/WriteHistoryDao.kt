package com.shexa.baseproject.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.shexa.baseproject.entities.WriteHistoryModel

@Dao
interface WriteHistoryDao
{
    @Query("select * from writeHistory")
    fun getAllWriteRecord() : List<WriteHistoryModel>

    @Insert
    fun insertWriteRecord(writeModel : WriteHistoryModel)

}