package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartSpendBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddCashClick: () -> Unit,
    unreviewedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val baseBg = androidx.compose.material3.MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    val activeAccent = if (isDark) Color(0xFF2AABEE) else Color(0xFF0284C7)
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF334155)

    val tabWidth = 84.dp
    val tabHeight = 50.dp
    val spacing = 6.dp
    val centerButtonWidth = 44.dp
    val startPadding = 6.dp

    // Tab 0 starts at x = 0dp inside the padded content box
    // Center button is at x = tabWidth + spacing = 84dp + 6dp = 90dp
    // Tab 1 starts at x = 90dp + centerButtonWidth + spacing = 90dp + 44dp + 6dp = 140dp
    val targetOffset = if (selectedTab == 0) 0.dp else 140.dp

    val indicatorOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_indicator_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.25f to baseBg.copy(alpha = 0.12f),
                    0.60f to baseBg.copy(alpha = 0.40f),
                    1.0f to baseBg.copy(alpha = 0.65f)
                )
            )
            .navigationBarsPadding()
            .padding(top = 16.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            shape = CircleShape, // Telegram-style stadium container roundness
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.height(64.dp).wrapContentWidth()
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = startPadding, vertical = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Telegram-style sliding stadium capsule highlight
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(tabHeight)
                        .clip(CircleShape) // Telegram-style stadium capsule roundness
                        .background(
                            if (isDark) Color(0xFF2AABEE).copy(alpha = 0.18f)
                            else Color(0xFF0284C7).copy(alpha = 0.12f)
                        )
                )

                // Row of tabs and center button on top
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab(
                        title = "Ledger",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        isSelected = selectedTab == 0,
                        activeAccent = activeAccent,
                        inactiveColor = inactiveColor,
                        onClick = { onTabSelected(0) },
                        modifier = Modifier
                            .width(tabWidth)
                            .height(tabHeight)
                    )

                    CenterNavActionButton(
                        onClick = onAddCashClick,
                        modifier = Modifier.size(centerButtonWidth)
                    )

                    NavTab(
                        title = "Insights",
                        icon = Icons.Default.Insights,
                        isSelected = selectedTab == 1,
                        activeAccent = activeAccent,
                        inactiveColor = inactiveColor,
                        onClick = { onTabSelected(1) },
                        modifier = Modifier
                            .width(tabWidth)
                            .height(tabHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterNavActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "center_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Cash Transaction",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeAccent: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeAccent else inactiveColor,
        animationSpec = tween(durationMillis = 220),
        label = "tab_content_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}
