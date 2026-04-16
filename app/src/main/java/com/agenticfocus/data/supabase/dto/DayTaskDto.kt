package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.DayTaskEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/** Handles both JSON boolean (true/false) and integer (0/1) from Supabase. */
object BooleanOrIntSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanOrInt", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean {
        if (decoder is JsonDecoder) {
            val elem = decoder.decodeJsonElement()
            if (elem is JsonPrimitive && !elem.isString) {
                val raw = elem.content
                if (raw == "true") return true
                if (raw == "false") return false
                raw.toIntOrNull()?.let { return it != 0 }
            }
            return false
        }
        return decoder.decodeBoolean()
    }
}

@Serializable
data class DayTaskDto(
    val id: String,
    val date: String?,
    val name: String,
    @SerialName("planned_pomodoros") val plannedPomodoros: Int,
    @SerialName("completed_pomodoros") val completedPomodoros: Int,
    val position: Int,
    @SerialName("template_id") val templateId: String?,
    @SerialName("domain_id") val domainId: String? = null,
    @SerialName("story_points") val storyPoints: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: Long,
    @Serializable(with = BooleanOrIntSerializer::class)
    @SerialName("is_completed") val isCompleted: Boolean = false,
    val source: String? = null,
    @SerialName("routine_item_id") val routineItemId: String? = null,
    val note: String? = null,
    val impact: String? = null,
    val urgency: String? = null,
    @SerialName("due_date") val dueDate: Long? = null
)

fun DayTaskDto.toEntity() = DayTaskEntity(
    id = id, date = date, name = name,
    plannedPomodoros = plannedPomodoros, completedPomodoros = completedPomodoros,
    position = position, templateId = templateId, domainId = domainId,
    storyPoints = storyPoints, createdAt = createdAt,
    userId = userId, updatedAt = updatedAt, isCompleted = isCompleted,
    source = source, routineItemId = routineItemId,
    note = note, impact = impact, urgency = urgency, dueDate = dueDate
)

fun DayTaskEntity.toDto(userId: String) = DayTaskDto(
    id = id,
    date = date,
    name = name,
    plannedPomodoros = plannedPomodoros,
    completedPomodoros = completedPomodoros,
    position = position,
    templateId = templateId,
    domainId = domainId,
    storyPoints = storyPoints,
    createdAt = createdAt,
    userId = userId,
    updatedAt = System.currentTimeMillis(),
    isCompleted = isCompleted,
    source = source,
    routineItemId = routineItemId,
    note = note,
    impact = impact,
    urgency = urgency,
    dueDate = dueDate
)
