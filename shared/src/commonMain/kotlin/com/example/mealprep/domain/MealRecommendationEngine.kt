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
        // BREAKFAST (Sarapan)
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
            id = "m3",
            name = "Smoothie Bowl Alpukat, Pisang & Whey Protein",
            slot = MealSlot.BREAKFAST,
            baseCalories = 460,
            baseProteinGrams = 30,
            baseCarbsGrams = 48,
            baseFatGrams = 14,
            prepMinutes = 8,
            highlights = listOf("Segar & Cepat", "Tinggi Protein"),
            baseIngredients = listOf("1 scoop whey protein", "1/2 buah alpukat", "1 buah pisang dingin", "150ml susu almond")
        ),
        BaseMealRecipe(
            id = "m4",
            name = "Dada Ayam Panggang, Ubi Kukus & Telur",
            slot = MealSlot.BREAKFAST,
            baseCalories = 500,
            baseProteinGrams = 36,
            baseCarbsGrams = 52,
            baseFatGrams = 12,
            prepMinutes = 15,
            highlights = listOf("Sarapan Atlet", "Kenyang Lama"),
            baseIngredients = listOf("100g dada ayam panggang", "150g ubi kuning kukus", "1 butir telur ceplok air", "Tomat ceri")
        ),

        // LUNCH (Makan Siang)
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
            id = "l3",
            name = "Sate Dada Ayam Bumbu Kacang Tipis & Nasi Merah",
            slot = MealSlot.LUNCH,
            baseCalories = 660,
            baseProteinGrams = 45,
            baseCarbsGrams = 70,
            baseFatGrams = 16,
            prepMinutes = 20,
            highlights = listOf("Sate Sehat", "Rendah Minyak"),
            baseIngredients = listOf("150g dada ayam fillet", "2 sdm bumbu kacang sangrai", "130g nasi merah", "Timun & bawang merah")
        ),
        BaseMealRecipe(
            id = "l4",
            name = "Salmon Panggang, Kentang Rebus & Salad Hijau",
            slot = MealSlot.LUNCH,
            baseCalories = 700,
            baseProteinGrams = 44,
            baseCarbsGrams = 60,
            baseFatGrams = 22,
            prepMinutes = 20,
            highlights = listOf("Kaya Lemak Sehat", "Omega-3 Tinggi"),
            baseIngredients = listOf("140g fillet salmon", "180g kentang rebus", "100g salad selada & lemon", "1 sdt dressing zaitun")
        ),

        // DINNER (Makan Malam)
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
        ),
        BaseMealRecipe(
            id = "d3",
            name = "Sup Ikan Gurame Bening, Tahu & Sayur Oyong",
            slot = MealSlot.DINNER,
            baseCalories = 480,
            baseProteinGrams = 36,
            baseCarbsGrams = 42,
            baseFatGrams = 12,
            prepMinutes = 25,
            highlights = listOf("Sup Bening Segar", "Sangat Low Fat"),
            baseIngredients = listOf("150g fillet ikan gurame", "100g tahu lembut", "100g oyong & jahe", "Daun bawang & seledri")
        ),
        BaseMealRecipe(
            id = "d4",
            name = "Tumis Daging Sapi Lada Hitam Lean & Brokoli",
            slot = MealSlot.DINNER,
            baseCalories = 580,
            baseProteinGrams = 42,
            baseCarbsGrams = 46,
            baseFatGrams = 18,
            prepMinutes = 18,
            highlights = listOf("Kaya Zat Besi", "Tinggi Protein"),
            baseIngredients = listOf("140g daging sapi bagian has dalam (tenderloin)", "120g brokoli", "Saus lada hitam tipis", "100g nasi merah")
        )
    )

    fun generateDayPlan(
        macroTarget: MacroTarget,
        excludeIds: Set<String> = emptySet(),
        randomize: Boolean = false
    ): RecommendedDayPlan {
        val scaledMeals = MealSlot.entries.mapNotNull { slot ->
            val slotTargetCal = (macroTarget.targetCalories * slot.calorieShare).roundToInt()
            val candidates = sampleRecipes.filter { it.slot == slot }
            val filtered = candidates.filterNot { it.id in excludeIds }.ifEmpty { candidates }

            val chosen = if (randomize) {
                filtered.shuffled().firstOrNull() ?: candidates.minByOrNull { abs(it.baseCalories - slotTargetCal) }
            } else {
                filtered.minByOrNull { abs(it.baseCalories - slotTargetCal) }
            } ?: return@mapNotNull null

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
