package com.example.mealprep.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
enum class MealSlot(val label: String, val calorieShare: Double) {
    BREAKFAST("Sarapan / Meal 1", 0.25),
    LUNCH("Makan Siang / Meal 2", 0.40),
    DINNER("Makan Malam / Meal 3", 0.35)
}

data class BaseMealRecipe(
    val id: String,
    val name: String,
    val slot: MealSlot,
    val baseCalories: Int,
    val baseProteinGrams: Int,
    val baseCarbsGrams: Int,
    val baseFatGrams: Int,
    val prepMinutes: Int,
    val highlights: List<String>,
    val baseIngredients: List<String>
)

@Serializable
data class ScaledMeal(
    val id: String,
    val name: String,
    val slot: MealSlot,
    val portionMultiplier: Double,
    val portionLabel: String,
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val prepMinutes: Int,
    val highlights: List<String>,
    val scaledIngredients: List<String>
)

@Serializable
data class RecommendedDayPlan(
    val targetCalories: Int,
    val totalPlannedCalories: Int,
    val totalProteinGrams: Int,
    val totalCarbsGrams: Int,
    val totalFatGrams: Int,
    val meals: List<ScaledMeal>
)

object MealRecommendationEngine {

    val sampleRecipes: List<BaseMealRecipe> = listOf(
        BaseMealRecipe(
            id = "m1",
            name = "Oatmeal Telur Rebus & Pisang",
            slot = MealSlot.BREAKFAST,
            baseCalories = 480,
            baseProteinGrams = 24,
            baseCarbsGrams = 56,
            baseFatGrams = 16,
            prepMinutes = 10,
            highlights = listOf("Tinggi Serat", "Praktis Pagi"),
            baseIngredients = listOf("60g rolled oat", "2 butir telur rebus", "1 buah pisang ambon", "200ml susu rendah lemak")
        ),
        BaseMealRecipe(
            id = "m2",
            name = "Scrambled Egg Tofu & Roti Gandum",
            slot = MealSlot.BREAKFAST,
            baseCalories = 440,
            baseProteinGrams = 22,
            baseCarbsGrams = 45,
            baseFatGrams = 18,
            prepMinutes = 12,
            highlights = listOf("High Protein", "Vegetarian Friendly"),
            baseIngredients = listOf("120g tahu putih", "2 butir telur", "2 lembar roti gandum utuh", "Daun bawang & lada")
        ),
        BaseMealRecipe(
            id = "l1",
            name = "Grilled Chicken Breast, Nasi Merah & Brokoli",
            slot = MealSlot.LUNCH,
            baseCalories = 680,
            baseProteinGrams = 48,
            baseCarbsGrams = 72,
            baseFatGrams = 18,
            prepMinutes = 25,
            highlights = listOf("Meal Prep Favorit", "Low Fat"),
            baseIngredients = listOf("160g dada ayam tanpa kulit", "130g nasi merah matang", "100g brokoli kukus", "1 sdt minyak zaitun")
        ),
        BaseMealRecipe(
            id = "l2",
            name = "Pepes Ikan Tongkol, Nasi Cokelat & Buncis",
            slot = MealSlot.LUNCH,
            baseCalories = 640,
            baseProteinGrams = 42,
            baseCarbsGrams = 68,
            baseFatGrams = 19,
            prepMinutes = 30,
            highlights = listOf("Rasa Nusantara", "Kaya Omega-3"),
            baseIngredients = listOf("150g ikan tongkol", "130g nasi cokelat matang", "90g buncis tumis ringan", "Bumbu pepes & daun pisang")
        ),
        BaseMealRecipe(
            id = "d1",
            name = "Dori Panggang Lemon, Kentang Rebus & Edamame",
            slot = MealSlot.DINNER,
            baseCalories = 560,
            baseProteinGrams = 38,
            baseCarbsGrams = 54,
            baseFatGrams = 19,
            prepMinutes = 20,
            highlights = listOf("Ringan Malam Hari", "Gluten-Free"),
            baseIngredients = listOf("160g fillet ikan dori", "180g kentang rebus", "75g edamame kupas", "Air perasan lemon & bawang putih")
        ),
        BaseMealRecipe(
            id = "d2",
            name = "Tumis Ayam Suwir Kemangi & Nasi Merah",
            slot = MealSlot.DINNER,
            baseCalories = 520,
            baseProteinGrams = 40,
            baseCarbsGrams = 48,
            baseFatGrams = 16,
            prepMinutes = 20,
            highlights = listOf("Wangi Kemangi", "Cocok Defisit"),
            baseIngredients = listOf("150g dada ayam suwir", "110g nasi merah matang", "1 ikat kecil daun kemangi", "Cabai, bawang & kaldu jamur")
        )
    )

    fun generateDayPlan(
        macroTarget: MacroTarget,
        excludeIds: Set<String> = emptySet()
    ): RecommendedDayPlan {
        val scaledMeals = MealSlot.entries.mapNotNull { slot ->
            val slotTargetCal = (macroTarget.targetCalories * slot.calorieShare).roundToInt()
            val candidates = sampleRecipes.filter { it.slot == slot }
            val filtered = candidates.filterNot { it.id in excludeIds }.ifEmpty { candidates }

            // Pilih resep yang paling mendekati target kalori slot tersebut
            val chosen = filtered.minByOrNull { abs(it.baseCalories - slotTargetCal) }
                ?: return@mapNotNull null

            val rawRatio = slotTargetCal.toDouble() / chosen.baseCalories.coerceAtLeast(1)
            val clampedRatio = rawRatio.coerceIn(0.6, 1.6)
            val roundedRatio = (clampedRatio * 10.0).roundToInt() / 10.0

            val scaledCalories = (chosen.baseCalories * roundedRatio).roundToInt()
            val scaledProtein = (chosen.baseProteinGrams * roundedRatio).roundToInt()
            val scaledCarbs = (chosen.baseCarbsGrams * roundedRatio).roundToInt()
            val scaledFat = (chosen.baseFatGrams * roundedRatio).roundToInt()

            val scaledIngredients = chosen.baseIngredients.map { ingredient ->
                scaleIngredientText(ingredient, roundedRatio)
            }

            ScaledMeal(
                id = chosen.id,
                name = chosen.name,
                slot = slot,
                portionMultiplier = roundedRatio,
                portionLabel = "Porsi ${roundedRatio}x",
                calories = scaledCalories,
                proteinGrams = scaledProtein,
                carbsGrams = scaledCarbs,
                fatGrams = scaledFat,
                prepMinutes = chosen.prepMinutes,
                highlights = chosen.highlights,
                scaledIngredients = scaledIngredients
            )
        }

        return RecommendedDayPlan(
            targetCalories = macroTarget.targetCalories,
            totalPlannedCalories = scaledMeals.sumOf { it.calories },
            totalProteinGrams = scaledMeals.sumOf { it.proteinGrams },
            totalCarbsGrams = scaledMeals.sumOf { it.carbsGrams },
            totalFatGrams = scaledMeals.sumOf { it.fatGrams },
            meals = scaledMeals
        )
    }

    private fun scaleIngredientText(text: String, multiplier: Double): String {
        if (abs(multiplier - 1.0) < 0.05) return text
        val match = Regex("""^(\d+(?:\.\d+)?)\s*(g|ml|butir|buah|lembar|sdt)\b(.*)$""").find(text.trim())
            ?: return text

        val amount = match.groupValues[1].toDoubleOrNull() ?: return text
        val unit = match.groupValues[2]
        val rest = match.groupValues[3]

        val scaledAmount = (amount * multiplier).roundToInt().coerceAtLeast(1)
        return "$scaledAmount $unit$rest"
    }
}