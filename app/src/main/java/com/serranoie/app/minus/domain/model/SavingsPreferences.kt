package com.serranoie.app.minus.domain.model

import java.math.BigDecimal

/**
 * User-configurable savings targets that drive the Savings Recommendation card
 * on the Analytics screen.
 *
 * [preset] is the user-selected split and is a *primary* field — it stores
 * what the user actually picked from the UI (including [SavingsSplitPreset.CUSTOM]
 * for hand-tuned values). The [needsPct] / [wantsPct] / [savingsPct] fields are
 * the percentages the card should render:
 *  - When [preset] is one of the named splits, the UI uses the preset's
 *    built-in percentages and the user cannot edit them.
 *  - When [preset] is [SavingsSplitPreset.CUSTOM], the user has picked Custom
 *    and is hand-tuning [needsPct] / [wantsPct] / [savingsPct] via the sliders.
 *
 * Defaults follow the classic 50/30/20 rule:
 * - 50% Needs   → [SavingsSplitPreset.BALANCED] default → [needsPct] = 50
 * - 30% Wants   → [SavingsSplitPreset.BALANCED] default → [wantsPct] = 30
 * - 20% Savings → [SavingsSplitPreset.BALANCED] default → [savingsPct] = 20
 *
 * The optional [savingsGoalAmount] + [savingsGoalMonths] turn the card's
 * "if you save X% for 6 months…" projection into a concrete goal:
 * "to reach $X in N months, save $Y per period". When either is null the
 * card falls back to the default 6-month example.
 */
data class SavingsPreferences(
    val preset: SavingsSplitPreset = SavingsSplitPreset.BALANCED,
    val needsPct: Int = preset.needsPct,
    val wantsPct: Int = preset.wantsPct,
    val savingsPct: Int = preset.savingsPct,
    val savingsGoalAmount: BigDecimal? = null,
    val savingsGoalMonths: Int? = null,
) {
    /** Threshold above which total spending is considered "in the red". */
    val spendingCeilingPct: Int get() = needsPct + wantsPct

    /**
     * Compute the per-period savings amount needed to reach
     * [savingsGoalAmount] in [savingsGoalMonths] periods. Returns null if
     * either field is null/zero (the card then falls back to the default
     * "20% × 6" projection).
     */
    fun projectedPerPeriod(): BigDecimal? {
        val goal = savingsGoalAmount ?: return null
        val months = savingsGoalMonths ?: return null
        if (months <= 0 || goal <= BigDecimal.ZERO) return null
        return goal.divide(BigDecimal(months), 2, java.math.RoundingMode.HALF_UP)
    }

    companion object {
        const val DEFAULT_NEEDS_PCT = 50
        const val DEFAULT_WANTS_PCT = 30
        const val DEFAULT_SAVINGS_PCT = 20

        /**
         * Default [SavingsPreferences] with the classic 50/30/20 rule and
         * no savings goal.
         */
        val DEFAULT = SavingsPreferences(
            preset = SavingsSplitPreset.BALANCED,
            needsPct = DEFAULT_NEEDS_PCT,
            wantsPct = DEFAULT_WANTS_PCT,
            savingsPct = DEFAULT_SAVINGS_PCT,
        )

        /**
         * Build a [SavingsPreferences] from a named [preset], keeping the
         * goal fields independent (the split and the goal are configured
         * separately). The percentages are taken from the preset.
         */
        fun fromPreset(
            preset: SavingsSplitPreset,
            savingsGoalAmount: BigDecimal? = null,
            savingsGoalMonths: Int? = null,
        ): SavingsPreferences = SavingsPreferences(
            preset = preset,
            needsPct = preset.needsPct,
            wantsPct = preset.wantsPct,
            savingsPct = preset.savingsPct,
            savingsGoalAmount = savingsGoalAmount,
            savingsGoalMonths = savingsGoalMonths,
        )
    }
}

/**
 * Named split presets surfaced in the Settings UI. The user picks one of
 * the named presets for a one-tap split, or [CUSTOM] to hand-tune the
 * percentages via sliders.
 */
enum class SavingsSplitPreset(
    val needsPct: Int,
    val wantsPct: Int,
    val savingsPct: Int,
) {
    /** Classic 50/30/20 — the default. */
    BALANCED(50, 30, 20),

    /** For aggressive savers / high-income earners: 40/20/40. */
    AGGRESSIVE_SAVER(40, 20, 40),

    /** For high cost-of-living or tight budgets: 70/20/10. */
    CONSERVATIVE(70, 20, 10),

    /** User-defined split; the exact values live in [SavingsPreferences]. */
    CUSTOM(50, 30, 20);

    companion object {
        /**
         * Match a (needs, wants, savings) triple to a named preset, falling
         * back to [CUSTOM] when the values don't match any preset.
         *
         * Used for backward-compatibility when reading legacy prefs that
         * only persisted the three percentage values without an explicit
         * [SavingsPreferences.preset] field.
         */
        fun fromValues(needsPct: Int, wantsPct: Int, savingsPct: Int): SavingsSplitPreset =
            entries.firstOrNull {
                it != CUSTOM &&
                    it.needsPct == needsPct &&
                    it.wantsPct == wantsPct &&
                    it.savingsPct == savingsPct
            } ?: CUSTOM
    }
}
