@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.bugreport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.permission.PermissionHandler
import com.serranoie.app.minus.presentation.ui.settings.bugreport.component.AttachmentThumbnailCard
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportIssueType
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiIntent
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import kotlinx.coroutines.delay

@Composable
fun BugReportForm(
    uiState: BugReportUiState,
    onIntent: (BugReportUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    permissionHandler: PermissionHandler = remember { PermissionHandler() },
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            permissionHandler.onAttachmentPickerResult(
                context = context,
                uris = uris,
                onAttachmentsSelected = { selectedUris ->
                    onIntent(BugReportUiIntent.SelectAttachments(selectedUris))
                }
            )
        }
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bug_report_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            HelpBanner(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(text = stringResource(R.string.bug_report_type_label))

            Spacer(modifier = Modifier.height(8.dp))

            IssueTypeSelector(
                selectedIssueType = uiState.selectedIssueType,
                onIssueTypeChange = { onIntent(BugReportUiIntent.SelectIssueType(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            FormField(
                label = stringResource(R.string.bug_report_title_label),
                placeholder = stringResource(R.string.bug_report_title_placeholder),
                value = uiState.title,
                onValueChange = { onIntent(BugReportUiIntent.ChangeTitle(it)) },
                minHeight = 56.dp,
                isError = uiState.description.isNotBlank() && !uiState.hasTitle,
                errorText = stringResource(R.string.bug_report_title_required),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            FormField(
                label = if (uiState.selectedIssueType == BugReportIssueType.FeatureRequest) {
                    stringResource(R.string.bug_report_problem_label)
                } else {
                    stringResource(R.string.bug_report_description_label)
                },
                placeholder = if (uiState.selectedIssueType == BugReportIssueType.FeatureRequest) {
                    stringResource(R.string.bug_report_problem_placeholder)
                } else {
                    stringResource(R.string.bug_report_description_placeholder)
                },
                value = uiState.description,
                onValueChange = { onIntent(BugReportUiIntent.ChangeDescription(it)) },
                minHeight = 118.dp,
                isError = uiState.hasTitle && !uiState.hasDescription,
                errorText = if (uiState.selectedIssueType == BugReportIssueType.FeatureRequest) {
                    stringResource(R.string.bug_report_problem_required)
                } else {
                    stringResource(R.string.bug_report_description_required)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.selectedIssueType == BugReportIssueType.BugReport) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel(text = stringResource(R.string.bug_report_include_steps_label))
                    Switch(
                        checked = uiState.showReproductionSteps,
                        onCheckedChange = { onIntent(BugReportUiIntent.ToggleReproductionSteps(it)) }
                    )
                }

                AnimatedVisibility(
                    visible = uiState.showReproductionSteps,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        StepsToReproduceField(
                            stepCount = uiState.reproductionSteps.size,
                            stepIdAt = { index -> uiState.reproductionSteps[index].id },
                            stepValueAt = { index -> uiState.reproductionSteps[index].value },
                            stepVisibleAt = { index -> uiState.reproductionSteps[index].visible },
                            onStepChange = { index, value ->
                                onIntent(BugReportUiIntent.ChangeStep(index, value))
                            },
                            onAddStep = {
                                onIntent(BugReportUiIntent.AddStep)
                            },
                            onRemoveStep = { index ->
                                onIntent(BugReportUiIntent.RemoveStep(index))
                            },
                            onStepExitFinish = { stepId ->
                                onIntent(BugReportUiIntent.FinishStepExit(stepId))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                FormField(
                    label = stringResource(R.string.bug_report_proposed_solution_label),
                    placeholder = stringResource(R.string.bug_report_proposed_solution_placeholder),
                    value = uiState.proposedSolution,
                    onValueChange = { onIntent(BugReportUiIntent.ChangeProposedSolution(it)) },
                    minHeight = 118.dp,
                    isError = uiState.hasTitle && !uiState.hasProposedSolution,
                    errorText = stringResource(R.string.bug_report_proposed_solution_required)
                )

                Spacer(modifier = Modifier.height(24.dp))

                FormField(
                    label = stringResource(R.string.bug_report_alternatives_considered_label),
                    placeholder = stringResource(R.string.bug_report_alternatives_considered_placeholder),
                    value = uiState.alternativesConsidered,
                    onValueChange = { onIntent(BugReportUiIntent.ChangeAlternativesConsidered(it)) },
                    minHeight = 94.dp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            FormField(
                label = stringResource(R.string.bug_report_additional_context_label),
                placeholder = if (uiState.selectedIssueType == BugReportIssueType.BugReport) {
                    stringResource(R.string.bug_report_additional_context_placeholder_bug)
                } else {
                    stringResource(R.string.bug_report_additional_context_placeholder_feature)
                },
                value = uiState.additionalInfo,
                onValueChange = { onIntent(BugReportUiIntent.ChangeAdditionalInfo(it)) },
                minHeight = 94.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel(
                text = if (uiState.selectedIssueType == BugReportIssueType.BugReport) {
                    stringResource(R.string.bug_report_attachments_label_bug)
                } else {
                    stringResource(R.string.bug_report_attachments_label)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AttachmentDropZone(
                attachments = uiState.selectedAttachmentUris,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    permissionHandler.requestAttachmentPickerPermissionIfNeeded(
                        attachmentPickerLauncher
                    )
                },
                onRemoveAttachment = { uri -> onIntent(BugReportUiIntent.RemoveAttachment(uri)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (uiState.canSubmit) {
                        onIntent(BugReportUiIntent.SubmitReport)
                    }
                },
                enabled = uiState.canSubmit,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    text = stringResource(R.string.bug_report_submit),
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
    }
}

@Composable
private fun IssueTypeSelector(
    selectedIssueType: BugReportIssueType,
    onIssueTypeChange: (BugReportIssueType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        stringResource(R.string.bug_report_type_bug_report),
        stringResource(R.string.bug_report_type_feature_request)
    )
    val issueTypes = listOf(
        BugReportIssueType.BugReport,
        BugReportIssueType.FeatureRequest,
    )
    val selectedIndex = issueTypes.indexOf(selectedIssueType).coerceAtLeast(0)
    val view = LocalView.current

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    view.confirmFeedback()
                    onIssueTypeChange(issueTypes[index])
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                    checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    checkedContentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label, modifier = Modifier.basicMarquee())
            }
        }
    }
}

@Composable
private fun HelpBanner(
    backgroundColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Help,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bug_report_help_title),
                style = MaterialTheme.typography.bodySmallCondensed,
                color = MaterialTheme.colorScheme.outline
            )
        }

        val helpMessagePrefix = stringResource(R.string.bug_report_help_message_prefix)
        val helpLinkLabel = stringResource(R.string.bug_report_help_github_issues)
        val helpMessageSuffix = stringResource(R.string.bug_report_help_message_suffix)
        val githubIssuesUrl = stringResource(R.string.bug_report_github_issues_url)

        Text(
            text = buildAnnotatedString {
                append(helpMessagePrefix)
                withLink(
                    LinkAnnotation.Url(
                        url = githubIssuesUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)
                        )
                    )
                ) {
                    append(helpLinkLabel)
                }
                append(helpMessageSuffix)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    rounded: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(220)) { height -> height / 2 } + fadeIn()).togetherWith(
                    slideOutVertically(animationSpec = tween(180)) { height -> -height / 2 } + fadeOut()
                )
            },
            label = "FormFieldLabelAnimation"
        ) { animatedLabel ->
            Text(
                text = animatedLabel,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = if (rounded) RoundedCornerShape(20.dp) else MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            minLines = if (singleLine) 1 else 4,
            singleLine = singleLine
        )

        AnimatedVisibility(visible = isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 16.dp)
            )
        }
    }
}

@Composable
private fun StepsToReproduceField(
    stepCount: Int,
    stepIdAt: (Int) -> Long,
    stepValueAt: (Int) -> String,
    stepVisibleAt: (Int) -> Boolean,
    onStepChange: (Int, String) -> Unit,
    onAddStep: () -> Unit,
    onRemoveStep: (Int) -> Unit,
    onStepExitFinish: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(240)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(stepCount) { index ->
            val stepId = stepIdAt(index)
            key(stepId) {
                AnimatedStepRow(
                    visible = stepVisibleAt(index),
                    stepId = stepId,
                    stepNumber = index + 1,
                    value = stepValueAt(index),
                    onValueChange = { onStepChange(index, it) },
                    onRemoveClick = { onRemoveStep(index) },
                    onExitFinish = onStepExitFinish
                )
            }
        }

        OutlinedButton(
            onClick = onAddStep,
            border = null,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bug_report_add_step),
                style = MaterialTheme.typography.labelLargeEmphasized
            )
        }
    }
}

@Composable
private fun AnimatedStepRow(
    visible: Boolean,
    stepId: Long,
    stepNumber: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    onExitFinish: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnExitFinish by rememberUpdatedState(onExitFinish)

    LaunchedEffect(visible, stepId) {
        if (!visible) {
            delay(240)
            currentOnExitFinish(stepId)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(180)) + expandVertically(
            animationSpec = tween(240),
            expandFrom = Alignment.Top
        ) + slideInVertically(animationSpec = tween(240)) { height -> height / 2 },
        exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(
            animationSpec = tween(220),
            shrinkTowards = Alignment.Top
        ) + slideOutVertically(animationSpec = tween(220)) { height -> -height / 3 },
        modifier = modifier
    ) {
        ReproductionStepRow(
            stepNumber = stepNumber,
            value = value,
            onValueChange = onValueChange,
            onRemoveClick = onRemoveClick
        )
    }
}

@Composable
private fun ReproductionStepRow(
    stepNumber: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val textStyle = MaterialTheme.typography.bodyMedium.merge(
        TextStyle(color = MaterialTheme.colorScheme.onSurface)
    )
    val placeholder = when (stepNumber) {
        1 -> stringResource(R.string.bug_report_step_placeholder_wallet)
        2 -> stringResource(R.string.bug_report_step_placeholder_transfer)
        3 -> stringResource(R.string.bug_report_step_placeholder_crash)
        else -> stringResource(R.string.bug_report_step_placeholder_format, stepNumber)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.bug_report_step_number_format, stepNumber),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(22.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            },
            textStyle = textStyle,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            singleLine = true,
        )

        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AttachmentDropZone(
    attachments: List<Uri>,
    onClick: () -> Unit,
    onRemoveAttachment: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 164.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            drawRoundRect(
                color = outlineColor,
                style = stroke,
                cornerRadius = CornerRadius(18.dp.toPx())
            )
        }

        if (attachments.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.bug_report_upload_assets),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.bug_report_upload_assets_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.bug_report_upload_assets_selected_count,
                        attachments.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AttachmentThumbnailRow(
                    attachments = attachments,
                    onRemove = onRemoveAttachment,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AttachmentThumbnailRow(
    attachments: List<Uri>,
    onRemove: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items = attachments, key = { it.toString() }) { uri ->
            AttachmentThumbnailCard(
                uri = uri,
                onRemove = { onRemove(uri) }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Preview(name = "BugReportForm", showBackground = true, device = "id:pixel_5")
@PreviewFontScale
@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun PreviewBugReportForm() {
    MinusTheme {
        BugReportForm(
            uiState = BugReportUiState(),
            onIntent = {},
        )
    }
}
