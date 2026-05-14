package com.agenticfocus.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class TagIdsListConverter {

    @TypeConverter
    fun fromList(value: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())
}
