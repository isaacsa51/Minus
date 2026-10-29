package com.serranoie.app.minus.presentation.shortcut

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.shortcut.LauncherShortcut
import com.serranoie.app.minus.presentation.MainActivity

private const val ID_ADD_EXPENSE = "add_expense"
private const val ID_SCAN_RECEIPT = "scan_receipt"
private const val ID_CATEGORY_PREFIX = "category:"

fun publishLauncherShortcuts(context: Context, categories: List<String>) {
    val fixed = listOf(
        shortcut(
            context,
            id = ID_ADD_EXPENSE,
            shortLabel = context.getString(R.string.widget_add_expense_compact_label),
            longLabel = context.getString(R.string.widget_add_expense_label),
            icon = R.drawable.ic_shortcut_add,
            intent = shortcutIntent(context, LauncherShortcut.ACTION_ADD_EXPENSE),
        ),
        shortcut(
            context,
            id = ID_SCAN_RECEIPT,
            shortLabel = context.getString(R.string.receipt_scan_button),
            icon = R.drawable.ic_shortcut_receipt,
            intent = shortcutIntent(context, LauncherShortcut.ACTION_SCAN_RECEIPT),
        ),
    )
    val recent = categories.map { name ->
        shortcut(
            context,
            id = ID_CATEGORY_PREFIX + name,
            shortLabel = name,
            icon = R.drawable.ic_shortcut_category,
            intent = shortcutIntent(context, LauncherShortcut.ACTION_CATEGORY)
                .putExtra(LauncherShortcut.EXTRA_CATEGORY, name),
        )
    }
    val all = (fixed + recent).mapIndexed { rank, info ->
        ShortcutInfoCompat.Builder(info).setRank(rank).build()
    }
    ShortcutManagerCompat.setDynamicShortcuts(context, all)
}

private fun shortcutIntent(context: Context, action: String) =
    Intent(context, MainActivity::class.java).setAction(action)

private fun shortcut(
    context: Context,
    id: String,
    shortLabel: String,
    @DrawableRes icon: Int,
    intent: Intent,
    longLabel: String = shortLabel,
) = ShortcutInfoCompat.Builder(context, id)
    .setShortLabel(shortLabel)
    .setLongLabel(longLabel)
    .setIcon(IconCompat.createWithResource(context, icon))
    .setIntent(intent)
    .build()
