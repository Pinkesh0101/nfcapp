package com.shexa.baseproject.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "writeHistory")
data class WriteHistoryModel(

    @ColumnInfo(name = "record_id")
    @PrimaryKey(autoGenerate = true)
    var recordId:Int?,
    @ColumnInfo(name = "record_content") var recordContent:String?,
    @ColumnInfo(name = "record_type") var recordType:String?
)