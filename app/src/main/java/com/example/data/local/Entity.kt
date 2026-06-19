package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chandeliers")
data class ChandelierEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val price: String,
    val imageUrl: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val image: String,
    val excerpt: String,
    val content: String,
    val date: String,
    val readingTime: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_design_requests")
data class CustomDesignRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val dimensions: String,
    val crystalType: String,
    val description: String,
    val isSubmitted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
