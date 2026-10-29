package com.serranoie.app.minus.domain.shortcut

import com.serranoie.app.minus.domain.model.Category as BudgetCategory

private const val MAX_CATEGORY_SHORTCUTS = 3

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

fun recentShortcutCategories(categories: List<BudgetCategory>): List<String> = categories
    .filter { !it.isHidden && it.lastUsedAt != null }
    .sortedByDescending { it.lastUsedAt }
    .take(MAX_CATEGORY_SHORTCUTS)
    .map { it.name }
