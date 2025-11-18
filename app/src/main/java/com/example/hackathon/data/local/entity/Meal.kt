package com.example.hackathon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "food_name")
    val foodName: String,

    @ColumnInfo(name = "calories")
    val calories: Int,

    @ColumnInfo(name = "protein")
    val protein: Float,

    @ColumnInfo(name = "fat")
    val fat: Float,

    @ColumnInfo(name = "carbs")
    val carbs: Float,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "image_path")
    val imagePath: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)