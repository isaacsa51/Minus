package com.serranoie.app.minus.domain.shortcut

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.Category
import org.junit.Test

class LauncherShortcutTest {

    @Test
    fun `scan action maps to ScanReceipt`() {
        assertThat(LauncherShortcut.from(LauncherShortcut.ACTION_SCAN_RECEIPT, null))
            .isEqualTo(LauncherShortcut.ScanReceipt)
    }

    @Test
    fun `category action carries the category name`() {
        assertThat(LauncherShortcut.from(LauncherShortcut.ACTION_CATEGORY, "Groceries"))
            .isEqualTo(LauncherShortcut.Category("Groceries"))
    }

    @Test
    fun `category action without a name is ignored`() {
        assertThat(LauncherShortcut.from(LauncherShortcut.ACTION_CATEGORY, " ")).isNull()
        assertThat(LauncherShortcut.from(LauncherShortcut.ACTION_CATEGORY, null)).isNull()
    }

    @Test
    fun `other actions are ignored`() {
        assertThat(LauncherShortcut.from("android.intent.action.MAIN", "Groceries")).isNull()
        assertThat(LauncherShortcut.from(null, null)).isNull()
    }

    @Test
    fun `recent categories are the last three used, newest first, skipping hidden and unused`() {
        val categories = listOf(
            Category(name = "Rent", usageCount = 9, lastUsedAt = 100),
            Category(name = "Coffee", usageCount = 2, lastUsedAt = 500),
            Category(name = "Hidden", isHidden = true, usageCount = 4, lastUsedAt = 900),
            Category(name = "Never", usageCount = 0, lastUsedAt = null),
            Category(name = "Groceries", usageCount = 5, lastUsedAt = 400),
            Category(name = "Fuel", usageCount = 1, lastUsedAt = 300),
        )

        assertThat(recentShortcutCategories(categories))
            .containsExactly("Coffee", "Groceries", "Fuel")
            .inOrder()
    }

    @Test
    fun `recent categories is empty when nothing was used`() {
        assertThat(recentShortcutCategories(listOf(Category(name = "New")))).isEmpty()
    }
}
