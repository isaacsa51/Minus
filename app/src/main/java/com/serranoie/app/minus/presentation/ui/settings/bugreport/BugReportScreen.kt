package com.serranoie.app.minus.presentation.ui.settings.bugreport

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiEffect

@Composable
fun BugReportScreen(
    onBack: () -> Unit,
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val shareTitle = stringResource(R.string.bug_report_share_title)
    val recipientEmail = stringResource(R.string.bug_report_recipient_email)
    val emailSubjectTemplate = stringResource(R.string.bug_report_email_subject)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BugReportUiEffect.OpenEmailComposer -> {
                    val mailToIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                    }
                    val defaultEmailPackage = mailToIntent.resolveActivity(context.packageManager)?.packageName
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        defaultEmailPackage?.let(::setPackage)
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                        putExtra(Intent.EXTRA_SUBJECT, emailSubjectTemplate.format(effect.title))
                        putExtra(Intent.EXTRA_TEXT, effect.body)
                        putExtra(Intent.EXTRA_STREAM, effect.uri)
                        putExtra(Intent.EXTRA_TITLE, effect.fileName)
                        clipData = ClipData.newUri(
                            context.contentResolver,
                            effect.fileName,
                            effect.uri,
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        context.startActivity(emailIntent)
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent.createChooser(emailIntent.setPackage(null), shareTitle))
                    }
                }
                is BugReportUiEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BugReportForm(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBackClick = onBack,
    )
}
