package fi.ville.treenipaivakirja

import androidx.annotation.StringRes
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Ravintolaskenta ----------
// Puhdas laskenta ilman Android-riippuvuuksia, jotta kaavat on helppo tarkistaa.

enum class Sex { MALE, FEMALE }

enum class Activity(val factor: Double, @StringRes val labelRes: Int, @StringRes val descRes: Int) {
    SEDENTARY(1.2, R.string.act_sedentary, R.string.act_sedentary_desc),
    LIGHT(1.375, R.string.act_light, R.string.act_light_desc),
    MODERATE(1.55, R.string.act_moderate, R.string.act_moderate_desc),
    HARD(1.725, R.string.act_hard, R.string.act_hard_desc),
    VERY_HARD(1.9, R.string.act_very_hard, R.string.act_very_hard_desc)
}

/** kcalFactor = kerroin kokonaiskulutukseen, proteinPerKg = proteiinitavoite painokiloa kohden. */
enum class Goal(val kcalFactor: Double, val proteinPerKg: Double, @StringRes val labelRes: Int) {
    CUT(0.80, 2.0, R.string.goal_cut),
    MAINTAIN(1.00, 1.8, R.string.goal_maintain),
    BULK(1.10, 1.8, R.string.goal_bulk)
}

const val KCAL_PROTEIN = 4.0
const val KCAL_CARB = 4.0
const val KCAL_FAT = 9.0
const val FAT_PER_KG = 0.9
/** Rasvaa vähintään tämä osuus kaloreista (hormonitoiminta, rasvaliukoiset vitamiinit). */
const val FAT_MIN_SHARE = 0.20

data class NutritionProfile(
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: Activity,
    val goal: Goal,
    val meals: Int,
    /** Käsin asetetut tavoitteet; null = lasketaan automaattisesti. Hiilihydraatit = loput kaloreista. */
    val manualKcal: Int? = null,
    val manualProtein: Int? = null,
    val manualFat: Int? = null
)

data class Macros(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(o: Macros) = Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
    operator fun div(n: Int) = Macros(kcal / n, protein / n, carbs / n, fat / n)
    companion object { val ZERO = Macros(0.0, 0.0, 0.0, 0.0) }
}

data class Targets(
    val bmr: Double,
    val tdee: Double,
    val daily: Macros,
    val perMeal: Macros,
    /** Tavoite alittaa perusaineenvaihdunnan. */
    val belowBmr: Boolean,
    /** Käsin annetut proteiini + rasva ylittävät kalorit -> hiilihydraatit nollassa. */
    val macrosExceedKcal: Boolean
)

/** Perusaineenvaihdunta, Mifflin-St Jeor (kcal/vrk). */
fun bmr(p: NutritionProfile): Double =
    10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.sex == Sex.MALE) 5 else -161

fun targets(p: NutritionProfile): Targets {
    val bmr = bmr(p)
    val tdee = bmr * p.activity.factor
    val kcal = (p.manualKcal?.toDouble() ?: (tdee * p.goal.kcalFactor)).roundToInt().toDouble()

    val protein = p.manualProtein?.toDouble() ?: (p.weightKg * p.goal.proteinPerKg).roundToInt().toDouble()
    val fat = p.manualFat?.toDouble()
        ?: max(p.weightKg * FAT_PER_KG, kcal * FAT_MIN_SHARE / KCAL_FAT).roundToInt().toDouble()
    val carbKcal = kcal - protein * KCAL_PROTEIN - fat * KCAL_FAT
    val carbs = max(0.0, carbKcal / KCAL_CARB).roundToInt().toDouble()

    val daily = Macros(kcal, protein, carbs, fat)
    return Targets(
        bmr = bmr,
        tdee = tdee,
        daily = daily,
        perMeal = daily / p.meals.coerceAtLeast(1),
        belowBmr = kcal < bmr,
        macrosExceedKcal = carbKcal < 0
    )
}

/** Ravintoarvot grammamäärälle, kun arvot on annettu per 100 g. */
fun macrosFor(grams: Double, kcal100: Double, p100: Double, c100: Double, f100: Double): Macros {
    val k = grams / 100.0
    return Macros(kcal100 * k, p100 * k, c100 * k, f100 * k)
}
