package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val expression by viewModel.expression.collectAsState()
    val result by viewModel.result.collectAsState()
    val historyList by viewModel.historyList.collectAsState()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val context = LocalContext.current

    // Absolute black canvas background conforming to edge-to-edge
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isTablet) {
            // Adaptive Tablet Layout: Side-by-side Bento Dashboard
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Calculator Display & Keypad Grid Bento Cards
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Block
                    AppTitleHeader(
                        historyCount = historyList.size,
                        onExport = { exportHistory(context, historyList) },
                        onClearAll = { viewModel.onAction(CalculatorAction.ClearHistory) }
                    )

                    // Main Display Card
                    DisplayBentoCard(
                        expression = expression,
                        result = result,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Keypad Bento Box
                    KeypadBentoCard(
                        onAction = viewModel::onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }

                // Right Column: Dedicated Scrollable History Bento Card
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .background(Color(0xFF111111), shape = RoundedCornerShape(24.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    HistoryPanelContent(
                        historyList = historyList,
                        onDelete = { viewModel.onAction(CalculatorAction.DeleteHistoryItem(it)) },
                        onClearAll = { viewModel.onAction(CalculatorAction.ClearHistory) },
                        onUseHistory = { entry ->
                            viewModel.onAction(CalculatorAction.SetExpression(entry.expression, entry.result))
                        },
                        onExport = { exportHistory(context, historyList) }
                    )
                }
            }
        } else {
            // Mobile Layout: Strictly vertical, stacked Bento Boxes
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                val availableHeight = maxHeight
                
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Navigation / Title Header Area
                    AppTitleHeader(
                        historyCount = historyList.size,
                        onExport = { exportHistory(context, historyList) },
                        onClearAll = { viewModel.onAction(CalculatorAction.ClearHistory) }
                    )

                    // 2. Calculation History Bento Card (Tops 24% of vertical screen)
                    RecentHistoryBentoCard(
                        historyList = historyList,
                        onDelete = { viewModel.onAction(CalculatorAction.DeleteHistoryItem(it)) },
                        onUseHistory = { entry ->
                            viewModel.onAction(CalculatorAction.SetExpression(entry.expression, entry.result))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(availableHeight * 0.24f)
                    )

                    // 3. Main Result Display Bento Card (Middle 22% of space)
                    DisplayBentoCard(
                        expression = expression,
                        result = result,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(availableHeight * 0.22f)
                    )

                    // 4. Compact Keypad Bento Grid (Bottom 54% space)
                    KeypadBentoCard(
                        onAction = viewModel::onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AppTitleHeader(
    historyCount: Int,
    onExport: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stylized tactile "=" key logo representing N-Calc Pro
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(NeonYellow, shape = RoundedCornerShape(8.dp))
                    .border(width = 1.3.dp, color = DarkYellow, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "＝",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Column {
                Text(
                    text = "N-CALC PRO",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        shadow = Shadow(color = NeonYellow.copy(alpha = 0.5f), blurRadius = 8f)
                    ),
                    color = BrightWhite.copy(alpha = 0.95f)
                )
                Text(
                    text = "BENTO SPEC",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    ),
                    color = BrightWhite.copy(alpha = 0.4f)
                )
            }
        }

        // Integrated actions inside the title bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Share chooser Action
            GlowRoundButton(
                onClick = onExport,
                icon = Icons.Default.Share,
                contentDescription = "Share Log Output",
                testTag = "export_history_button"
            )

            // Clear Log Action
            GlowRoundButton(
                onClick = onClearAll,
                icon = Icons.Default.Delete,
                contentDescription = "Clear All Calculations",
                tint = Color.Red.copy(alpha = 0.8f),
                testTag = "clear_history_button"
            )
        }
    }
}

@Composable
fun GlowRoundButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = BrightWhite.copy(alpha = 0.8f),
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(50))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(50))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun RecentHistoryBentoCard(
    historyList: List<HistoryEntry>,
    onDelete: (Long) -> Unit,
    onUseHistory: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .background(Color(0xFF111111), shape = RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Bento Header indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT HISTORY",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    ),
                    color = BrightWhite.copy(alpha = 0.4f)
                )

                Text(
                    text = "LOG:${String.format("%03d", historyList.size)}",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = NeonYellow
                )
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "History log is empty",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = BrightWhite.copy(alpha = 0.25f)
                    )
                }
            } else {
                // Scrollable micro bento log
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(historyList, key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUseHistory(entry)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.expression,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = BrightWhite.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "= ${entry.result}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = NeonYellow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Minimalist click container to remove
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete item",
                                tint = Color.Red.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDelete(entry.id)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayBentoCard(
    expression: String,
    result: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(26.dp)
            )
            .clip(RoundedCornerShape(26.dp))
            .drawBehind {
                // Decorative Ambient Yellow Neon Blur circle top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonYellow.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.95f, -size.height * 0.1f),
                        radius = size.width * 0.55f
                    )
                )

                // Render subtle CRT lines across background
                val step = 5.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.015f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += step
                }
            }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            // Expression Line (Muted White glowing digits, scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopEnd
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (expression.length > 15) 20.sp else 26.sp,
                        letterSpacing = 1.sp,
                        shadow = Shadow(
                            color = BrightWhite.copy(alpha = 0.4f),
                            blurRadius = 12f
                        )
                    ),
                    color = BrightWhite.copy(alpha = 0.6f),
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Neon Glowing Result Output with custom visual pairings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom decorative bento indicator block
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .background(NeonYellow, shape = RoundedCornerShape(50))
                )

                Text(
                    text = result,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = if (result.length > 9) 34.sp else 46.sp,
                        letterSpacing = 2.sp, // stylish spacing
                        shadow = Shadow(
                            color = NeonYellow,
                            offset = Offset(0f, 0f),
                            blurRadius = 20f
                        )
                    ),
                    color = BrightWhite, // Neon pop core contrasting yellow blur
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("result_text")
                )
            }
        }
    }
}

@Composable
fun KeypadBentoCard(
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant Container of grid items layout
    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Row 1: AC, ToggleSign, %, division
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BentoGridButton(text = "AC", onClick = { onAction(CalculatorAction.Clear) }, modifier = Modifier.weight(1f), isFunctional = true)
                BentoGridButton(text = "±", onClick = { onAction(CalculatorAction.ToggleSign) }, modifier = Modifier.weight(1f), isFunctional = true)
                BentoGridButton(text = "%", onClick = { onAction(CalculatorAction.Percent) }, modifier = Modifier.weight(1f), isFunctional = true)
                BentoGridButton(text = "÷", onClick = { onAction(CalculatorAction.Operator("÷")) }, modifier = Modifier.weight(1f), isOperator = true)
            }

            // Row 2: 7, 8, 9, multiplication
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BentoGridButton(text = "7", onClick = { onAction(CalculatorAction.Digit(7)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "8", onClick = { onAction(CalculatorAction.Digit(8)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "9", onClick = { onAction(CalculatorAction.Digit(9)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "×", onClick = { onAction(CalculatorAction.Operator("×")) }, modifier = Modifier.weight(1f), isOperator = true)
            }

            // Row 3: 4, 5, 6, subtraction
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BentoGridButton(text = "4", onClick = { onAction(CalculatorAction.Digit(4)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "5", onClick = { onAction(CalculatorAction.Digit(5)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "6", onClick = { onAction(CalculatorAction.Digit(6)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "−", onClick = { onAction(CalculatorAction.Operator("-")) }, modifier = Modifier.weight(1f), isOperator = true)
            }

            // Row 4: 1, 2, 3, addition
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BentoGridButton(text = "1", onClick = { onAction(CalculatorAction.Digit(1)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "2", onClick = { onAction(CalculatorAction.Digit(2)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "3", onClick = { onAction(CalculatorAction.Digit(3)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "+", onClick = { onAction(CalculatorAction.Operator("+")) }, modifier = Modifier.weight(1f), isOperator = true)
            }

            // Row 5: Parentheses, 0, dot, evaluate
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                BentoGridButton(text = "⌫", onClick = { onAction(CalculatorAction.Backspace) }, modifier = Modifier.weight(1f), isFunctional = true)
                BentoGridButton(text = "0", onClick = { onAction(CalculatorAction.Digit(0)) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = ".", onClick = { onAction(CalculatorAction.Decimal) }, modifier = Modifier.weight(1f))
                BentoGridButton(text = "=", onClick = { onAction(CalculatorAction.Calculate) }, modifier = Modifier.weight(1f), isOperator = true, hasBottomBevel = true)
            }
        }
    }
}

@Composable
fun BentoGridButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isFunctional: Boolean = false,
    hasBottomBevel: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Key tactile physics offset
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "ButtonPress"
    )

    // Layout values aligned with Tailwind spec sheet
    val backColor = when {
        isOperator -> NeonYellow
        isFunctional -> Color.White.copy(alpha = 0.05f)
        else -> Color(0xFF1A1A1A)
    }

    val glowRefColor = when {
        isOperator -> NeonYellow
        isFunctional -> NeonYellow.copy(alpha = 0.6f)
        else -> BrightWhite
    }

    val textColor = when {
        isOperator -> Color.Black
        isFunctional -> NeonYellow
        else -> BrightWhite
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null, 
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .testTag("btn_$text")
    ) {
        // 1. Backing 3D bottom bevel extrusion
        if (isOperator || hasBottomBevel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 4.dp)
                    .background(
                        color = if (isOperator) DarkYellow else Color(0xFF0F0F10),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        // 2. Clickable active front plate
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = pressOffsetY)
                .background(
                    color = backColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isPressed) glowRefColor else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isOperator) FontWeight.Black else FontWeight.Bold,
                    fontSize = if (text.length > 2) 13.sp else 21.sp,
                    letterSpacing = 1.sp,
                    shadow = if (isOperator) null else Shadow(
                        color = glowRefColor.copy(alpha = 0.35f),
                        blurRadius = 8f
                    )
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun HistoryPanelContent(
    historyList: List<HistoryEntry>,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onUseHistory: (HistoryEntry) -> Unit,
    onExport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${historyList.size} Record(s)",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = BrightWhite.copy(alpha = 0.4f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onExport,
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonYellow)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPORT", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                }

                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CLEAR ALL", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 8.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calculations recorded yet.",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    color = BrightWhite.copy(alpha = 0.25f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { entry ->
                    HistoryItemCard(
                        entry = entry,
                        onDelete = { onDelete(entry.id) },
                        onClick = { onUseHistory(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    entry: HistoryEntry,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = formatter.format(Date(entry.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalButton, shape = RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = NeonYellow.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {
            // Expression row
            Text(
                text = entry.expression,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                ),
                color = BrightWhite.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Result row
            Text(
                text = "= ${entry.result}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    shadow = Shadow(color = NeonYellow.copy(alpha = 0.2f), blurRadius = 4f)
                ),
                color = NeonYellow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Meta time row
            Text(
                text = timeString,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                color = BrightWhite.copy(alpha = 0.3f)
            )
        }

        // Trash Button overlay on the absolute right
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp)
                .testTag("delete_history_item_${entry.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete item",
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

fun exportHistory(context: Context, historyList: List<HistoryEntry>) {
    if (historyList.isEmpty()) {
        Toast.makeText(context, "History is empty", Toast.LENGTH_SHORT).show()
        return
    }
    
    val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val text = StringBuilder().apply {
        append("==== NEON CALC HISTORY EXPORT ($dateText) ====\n\n")
        historyList.forEachIndexed { index, entry ->
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
            append("${index + 1}. [$timeStr] ${entry.expression} = ${entry.result}\n")
        }
        append("\n============================================\n")
    }.toString()

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Neon Calc History Log")
    }

    try {
        context.startActivity(Intent.createChooser(shareIntent, "Export Calculations History"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export", Toast.LENGTH_SHORT).show()
    }
}
