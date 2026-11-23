package io.github.xiaoaiswitch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Snackbar
import top.yukonga.miuix.kmp.basic.SnackbarDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.SinkFeedback

private val rowPress = SinkFeedback(sinkAmount = .98f)
private val buttonPress = SinkFeedback(sinkAmount = .96f)

@Composable
internal fun AppTheme(content: @Composable () -> Unit) {
    MiuixTheme(colors = if (isSystemInDarkTheme()) darkColorScheme(errorContainer = Color(0xFF382024), onErrorContainer = Color(0xFFFFB4AB))
        else lightColorScheme(errorContainer = Color(0xFFFFECEC), onErrorContainer = Color(0xFF9C2B2B)), content = content)
}

private data class StatusColors(val container: Color, val content: Color, val symbol: Color)

// Miuix has error roles; activation success gets its own light/dark semantic pair.
@Composable
private fun activationColors(active: Boolean): StatusColors {
    if (!active) return StatusColors(MiuixTheme.colorScheme.errorContainer, MiuixTheme.colorScheme.onErrorContainer, MiuixTheme.colorScheme.error)
    return if (isSystemInDarkTheme()) StatusColors(Color(0xFF1A3825), Color(0xFFB8E8C5), Color(0xFF36D167))
        else StatusColors(Color(0xFFDFFAE4), Color(0xFF164A29), Color(0xFF36D167))
}

@Composable
internal fun ActivationCard(active: Boolean, version: String, onClick: () -> Unit) {
    val colors = activationColors(active)
    Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(CardDefaults.CornerRadius)).testTag("activation_card"),
        onClick = onClick, showIndication = true, pressFeedbackType = PressFeedbackType.Tilt,
        colors = CardDefaults.defaultColors(color = colors.container, contentColor = colors.content)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 27.dp, y = 31.dp).size(150.dp)) {
                val tint = colors.symbol
                drawCircle(tint, radius = size.minDimension * .40f,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                if (active) {
                    val check = Path().apply {
                        moveTo(size.width * .29f, size.height * .50f)
                        lineTo(size.width * .44f, size.height * .65f)
                        lineTo(size.width * .73f, size.height * .36f)
                    }
                    drawPath(check, tint, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawLine(tint, Offset(size.width * .35f, size.height * .35f), Offset(size.width * .65f, size.height * .65f), 8.dp.toPx(), StrokeCap.Round)
                    drawLine(tint, Offset(size.width * .65f, size.height * .35f), Offset(size.width * .35f, size.height * .65f), 8.dp.toPx(), StrokeCap.Round)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 188.dp).padding(22.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.symbol.copy(alpha = .16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Xposed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.content)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (active) "已激活" else "未激活", fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.testTag("activation_status"))
                    Text("v$version", fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.testTag("app_version"))
                    if (!active) Text("启用模块并重启小布", fontSize = 13.sp, color = colors.content)
                }
            }
        }
    }
}

@Composable
internal fun SaveFeedbackHost(state: SnackbarHostState, error: Boolean) {
    val colors = activationColors(!error)
    SnackbarHost(state) { data ->
        Snackbar(data, modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("save_feedback")
            .semantics { liveRegion = LiveRegionMode.Polite },
            colors = SnackbarDefaults.snackbarColors(containerColor = colors.content, contentColor = colors.container,
                actionContentColor = colors.container, dismissActionContentColor = colors.container))
    }
}

@Composable
internal fun accountStatus(): State<CodexAccounts.Status?> {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState<CodexAccounts.Status?>(null, context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = withContext(Dispatchers.IO) { CodexAccounts.get(context).status() }
                delay(1000)
            }
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, modifier = Modifier.padding(start = 12.dp), fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
        }
    }
}

@Composable
internal fun SwitchRow(title: String, summary: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(summary, fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        Switch(checked, onChange, modifier = Modifier.testTag(tag).semantics { contentDescription = title })
    }
}

@Composable
internal fun Field(label: String, value: String, tag: String, prompt: Boolean = false, secret: Boolean = false,
                   keyboardType: KeyboardType = if (secret) KeyboardType.Password else KeyboardType.Text,
                   onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        TextField(value, onChange, label = if (prompt) "为此通道编写独立的模型指令" else label,
            useLabelAsPlaceholder = true, singleLine = !prompt, minLines = if (prompt) 5 else 1,
            modifier = Modifier.fillMaxWidth().testTag(tag).semantics { contentDescription = label },
            backgroundColor = MiuixTheme.colorScheme.surface,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
    }
}

@Composable
internal fun ChoiceRow(title: String, value: String, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    BasicComponent(title = title, summary = value, onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.testTag(tag).indication(interaction, rowPress).clip(RoundedCornerShape(14.dp)),
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        endActions = { NavigationArrow(back = false, color = MiuixTheme.colorScheme.onSurfaceVariantActions) })
}

@Composable
internal fun BackButton(tag: String, onClick: () -> Unit) {
    Box(Modifier.padding(start = 14.dp)) {
        IconButton(onClick = onClick, backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
            minWidth = 48.dp, minHeight = 48.dp, cornerRadius = 24.dp,
            modifier = Modifier.testTag(tag).semantics { contentDescription = "返回" }) {
            NavigationArrow(back = true, color = MiuixTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun NavigationArrow(back: Boolean, color: Color) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(if (back) 24.dp else 18.dp)) {
        val tip = if (back != rtl) .25f else .75f
        val wing = if (tip < .5f) .53f else .47f
        val arrow = Path().apply {
            moveTo(size.width * wing, size.height * .25f)
            lineTo(size.width * tip, size.height * .5f)
            lineTo(size.width * wing, size.height * .75f)
        }
        val strokeWidth = 2.dp.toPx()
        drawPath(arrow, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (back) {
            drawLine(color, Offset(size.width * tip, size.height * .5f),
                Offset(size.width * (1f - tip), size.height * .5f), strokeWidth, StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChoiceDialog(title: String, choices: List<String>, selected: String? = null,
                          onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    WindowDialog(show = true, title = title, onDismissRequest = onDismiss,
        modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        Column(modifier = Modifier.sizeIn(maxHeight = 440.dp).verticalScroll(rememberScrollState())) {
            choices.forEach { name ->
                val interaction = remember { MutableInteractionSource() }
                BasicComponent(title = name, onClick = { onSelect(name); onDismiss() },
                    modifier = Modifier.indication(interaction, rowPress).clip(RoundedCornerShape(14.dp)),
                    interactionSource = interaction,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                    endActions = { if (selected == name) Text("✓", color = MiuixTheme.colorScheme.primary) })
            }
        }
        TextButton("取消", onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
    }
}

@Composable
internal fun PrimaryButton(text: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(text, onClick, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().testTag(tag).indication(interaction, buttonPress),
        colors = ButtonDefaults.textButtonColorsPrimary())
}
