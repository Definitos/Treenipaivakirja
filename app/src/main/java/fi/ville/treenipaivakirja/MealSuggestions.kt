package fi.ville.treenipaivakirja

import androidx.annotation.StringRes
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- Ateriaehdotukset ----------
// Valmiit ruokakokonaisuudet, joiden määrät skaalataan aterian jäljellä olevaan tavoitteeseen.
// Ravintoarvot haetaan Finelistä id:n perusteella; painot ovat raakapainoja (riisi ja pasta kuivana).

enum class MealKind(@StringRes val labelRes: Int) {
    HOT(R.string.kind_hot),
    LIGHT(R.string.kind_light)
}

/**
 * Kokonaisuuden osa. min == max = kiinteä määrä (esim. kasvikset 200 g).
 * step = pyöristys, esim. kananmuna 55 g = 1 kpl, ruisleipä 35 g = 1 viipale.
 */
data class Part(
    val fineliId: Int, @StringRes val labelRes: Int, val min: Int, val max: Int, val step: Int,
    /** step on yksi kappale (kananmuna, leipäviipale) -> näytetään myös kappalemäärä */
    val piece: Boolean = false
) {
    val fixed get() = min == max
}

data class Combo(val key: String, val kind: MealKind, val parts: List<Part>)

private fun fixed(id: Int, label: Int, g: Int) = Part(id, label, g, g, 1)

val COMBOS: List<Combo> = listOf(
    // --- Lämpimät ateriat ---
    Combo("rice_mince", MealKind.HOT, listOf(
        Part(11521, R.string.ing_rice, 30, 160, 5),
        Part(32704, R.string.ing_beef_mince_9, 80, 300, 10),
        fixed(32157, R.string.ing_veg_mix, 200),
        Part(535, R.string.ing_oil, 0, 15, 5)
    )),
    Combo("rice_chicken_wok", MealKind.HOT, listOf(
        Part(11521, R.string.ing_rice, 30, 160, 5),
        Part(11565, R.string.ing_chicken, 80, 300, 10),
        fixed(30213, R.string.ing_wok_veg, 200),
        Part(535, R.string.ing_oil, 0, 15, 5)
    )),
    Combo("potato_salmon", MealKind.HOT, listOf(
        Part(205, R.string.ing_potato, 100, 600, 20),
        Part(871, R.string.ing_salmon, 80, 250, 10),
        fixed(32157, R.string.ing_veg_mix, 200)
    )),
    Combo("pasta_mince", MealKind.HOT, listOf(
        Part(121, R.string.ing_pasta, 30, 160, 5),
        Part(712, R.string.ing_beef_mince_7, 80, 300, 10),
        fixed(398, R.string.ing_crushed_tomato, 150),
        Part(535, R.string.ing_oil, 0, 15, 5)
    )),
    Combo("sweetpotato_chicken", MealKind.HOT, listOf(
        Part(30223, R.string.ing_sweet_potato, 100, 600, 20),
        Part(11565, R.string.ing_chicken, 80, 300, 10),
        fixed(29087, R.string.ing_salad, 150),
        Part(535, R.string.ing_oil, 0, 20, 5)
    )),
    Combo("quinoa_saithe", MealKind.HOT, listOf(
        Part(31175, R.string.ing_quinoa, 30, 150, 5),
        Part(826, R.string.ing_saithe, 100, 350, 10),
        fixed(32157, R.string.ing_veg_mix, 200),
        Part(535, R.string.ing_oil, 0, 20, 5)
    )),
    Combo("brownrice_salmon", MealKind.HOT, listOf(
        Part(28910, R.string.ing_brown_rice, 30, 160, 5),
        Part(871, R.string.ing_salmon, 80, 250, 10),
        fixed(30213, R.string.ing_wok_veg, 200)
    )),
    Combo("potato_mince_salad", MealKind.HOT, listOf(
        Part(205, R.string.ing_potato, 100, 600, 20),
        Part(32704, R.string.ing_beef_mince_9, 80, 300, 10),
        fixed(29087, R.string.ing_salad, 150),
        Part(535, R.string.ing_oil, 0, 15, 5)
    )),
    // --- Aamupala ja välipala ---
    Combo("oats_quark_berries", MealKind.LIGHT, listOf(
        Part(153, R.string.ing_oats, 20, 120, 5),
        Part(622, R.string.ing_quark, 100, 400, 25),
        fixed(454, R.string.ing_berries, 100),
        Part(379, R.string.ing_almonds, 0, 30, 5)
    )),
    Combo("porridge_eggs", MealKind.LIGHT, listOf(
        Part(153, R.string.ing_oats, 20, 120, 5),
        fixed(606, R.string.ing_skim_milk, 200),
        Part(858, R.string.ing_egg, 55, 220, 55, piece = true)
    )),
    Combo("rye_eggs_cheese", MealKind.LIGHT, listOf(
        Part(1009, R.string.ing_rye_bread, 35, 140, 35, piece = true),
        Part(858, R.string.ing_egg, 55, 220, 55, piece = true),
        Part(643, R.string.ing_cheese, 0, 40, 10),
        fixed(352, R.string.ing_tomato, 100)
    )),
    Combo("rye_cottage", MealKind.LIGHT, listOf(
        Part(1009, R.string.ing_rye_bread, 35, 140, 35, piece = true),
        Part(635, R.string.ing_cottage, 50, 300, 25),
        fixed(346, R.string.ing_cucumber, 100),
        Part(643, R.string.ing_cheese, 0, 30, 10)
    )),
    Combo("quark_banana_almond", MealKind.LIGHT, listOf(
        Part(622, R.string.ing_quark, 100, 500, 25),
        Part(11049, R.string.ing_banana, 0, 250, 25),
        Part(379, R.string.ing_almonds, 0, 40, 5)
    ))
)

data class SuggestedPart(val part: Part, val grams: Double, val per100: Macros) {
    val macros get() = macrosFor(grams, per100.kcal, per100.protein, per100.carbs, per100.fat)
}

data class Suggestion(val combo: Combo, val parts: List<SuggestedPart>, val total: Macros, val error: Double)

private fun steps(p: Part): List<Int> =
    if (p.fixed) listOf(p.min) else (p.min..p.max step p.step).toList()

/**
 * Hakee jokaiselle kokonaisuudelle määrät, jotka osuvat lähimmäs tavoitetta.
 * Virhe = painotettu suhteellinen poikkeama (proteiini painavin), joten pieni ylitys
 * yhdessä makrossa on parempi kuin selvä vaje proteiinissa.
 * Muuttuvia osia on enintään kolme, joten kattava ruudukkohaku on nopea (< 10 000 vaihtoehtoa per kokonaisuus).
 */
fun suggestMeals(target: Macros, per100: Map<Int, Macros>, kind: MealKind? = null): List<Suggestion> {
    val tk = max(target.kcal, 50.0)
    val tp = max(target.protein, 5.0)
    val tc = max(target.carbs, 5.0)
    val tf = max(target.fat, 3.0)

    fun error(m: Macros): Double {
        fun sq(v: Double, t: Double) = ((v - t) / t).let { it * it }
        return 1.5 * sq(m.protein, tp) + sq(m.carbs, tc) + sq(m.fat, tf) + 2.0 * sq(m.kcal, tk)
    }

    return COMBOS
        .filter { kind == null || it.kind == kind }
        .mapNotNull { combo ->
            val vals = combo.parts.map { per100[it.fineliId] ?: return@mapNotNull null }
            val options = combo.parts.map(::steps)
            var best: List<Int>? = null
            var bestErr = Double.MAX_VALUE
            // Kartesiaaninen tulo osien määristä
            fun search(i: Int, chosen: IntArray, acc: Macros) {
                if (i == options.size) {
                    val e = error(acc)
                    if (e < bestErr) { bestErr = e; best = chosen.toList() }
                    return
                }
                for (g in options[i]) {
                    chosen[i] = g
                    val v = vals[i]
                    search(i + 1, chosen, acc + macrosFor(g.toDouble(), v.kcal, v.protein, v.carbs, v.fat))
                }
            }
            search(0, IntArray(options.size), Macros.ZERO)
            val grams = best ?: return@mapNotNull null
            val parts = combo.parts.indices
                .filter { grams[it] > 0 }
                .map { SuggestedPart(combo.parts[it], grams[it].toDouble(), vals[it]) }
            Suggestion(combo, parts, parts.fold(Macros.ZERO) { a, p -> a + p.macros }, bestErr)
        }
        .sortedBy { it.error }
}

/** Kuinka hyvin ehdotus osuu (0–100 %), näytetään käyttäjälle. */
fun Suggestion.matchPercent(): Int = (100 / (1 + error * 4)).roundToInt()
