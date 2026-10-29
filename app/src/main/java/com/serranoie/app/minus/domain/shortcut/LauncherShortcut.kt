package com.serranoie.app.minus.domain.shortcut

import com.serranoie.app.minus.domain.model.Category as BudgetCategory

private const val MAX_CATEGORY_SHORTCUTS = 3
private const val MAX_QUICK_ADD_CATEGORIES = 10

sealed interface LauncherShortcut {
    data object ScanReceipt : LauncherShortcut
    data class Category(val name: String) : LauncherShortcut

    companion object {
        const val ACTION_ADD_EXPENSE = "com.serranoie.app.minus.action.ADD_EXPENSE"
        const val ACTION_SCAN_RECEIPT = "com.serranoie.app.minus.action.SCAN_RECEIPT"
        const val ACTION_CATEGORY = "com.serranoie.app.minus.action.CATEGORY"
        const val EXTRA_CATEGORY = "shortcut_category"

        fun from(action: String?, category: String?): LauncherShortcut? = when (action) {
            ACTION_SCAN_RECEIPT -> ScanReceipt
            ACTION_CATEGORY -> category?.takeIf { it.isNotBlank() }?.let(::Category)
            else -> null
        }
    }
}

fun quickAddCategories(categories: List<BudgetCategory>): List<String> {
    val mostUsed = categories
        .filter { !it.isHidden }
        .sortedWith(compareByDescending<BudgetCategory> { it.usageCount }.thenBy { it.name })
        .map { it.name }
    return (recentShortcutCategories(categories) + mostUsed).distinct().take(MAX_QUICK_ADD_CATEGORIES)
}

fun normalizeQuickAmount(raw: String): String {
    val unified = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = unified.indexOf('.')
    return if (dot < 0) unified else unified.substring(0, dot + 1) + unified.substring(dot + 1).replace(".", "")
}

fun recentShortcutCategories(categories: List<BudgetCategory>): List<String> = categories
    .filter { !it.isHidden && it.lastUsedAt != null }
    .sortedByDescending { it.lastUsedAt }
    .take(MAX_CATEGORY_SHORTCUTS)
    .map { it.name }
