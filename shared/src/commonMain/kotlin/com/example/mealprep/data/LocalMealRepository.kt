package com.example.mealprep.data

import com.example.mealprep.domain.DailyRoutineState
import com.example.mealprep.domain.GoalType
import com.example.mealprep.domain.MacroTarget
import com.example.mealprep.domain.RecommendedDayPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedMealPlanTemplate(
    val id: String,
    val title: String,
    val dateCreatedIso: String,
    val plan: RecommendedDayPlan
)

@Serializable
data class AppPersistedData(
    val routineLogs: Map<String, DailyRoutineState> = emptyMap(),
    val latestGoal: GoalType? = null,
    val latestMacroTarget: MacroTarget? = null,
    val latestActivePlan: RecommendedDayPlan? = null,
    val savedTemplates: List<SavedMealPlanTemplate> = emptyList()
)

class LocalMealRepository(
    private val storage: KeyValueStorage = KeyValueStorage()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun loadPersistedData(): AppPersistedData {
        val rawJson = storage.getString(KEY_APP_DATA) ?: return AppPersistedData()
        return try {
            json.decodeFromString<AppPersistedData>(rawJson)
        } catch (_: Exception) {
            AppPersistedData()
        }
    }

    fun savePersistedData(data: AppPersistedData) {
        try {
            val rawJson = json.encodeToString(data)
            storage.putString(KEY_APP_DATA, rawJson)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_APP_DATA = "key_app_persisted_meal_data_v2"
    }
}
