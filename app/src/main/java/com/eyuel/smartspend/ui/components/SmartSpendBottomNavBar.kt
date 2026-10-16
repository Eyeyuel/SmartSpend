package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.height(60.dp).wrapContentWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    title = "Ledger",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    isSelected = selectedTab == 0,
                    badgeCount = unreviewedCount,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.wrapContentWidth().padding(horizontal = 8.dp)
                )

                CenterNavActionButton(
                    onClick = onAddCashClick,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                NavBarItem(
                    title = "Insights",
                    icon = Icons.Default.Insights,
                    isSelected = selectedTab == 1,
                    badgeCount = 0,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.wrapContentWidth().padding(horizontal = 8.dp)
                )
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
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF10B981))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            ),
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

@Composable
private fun NavBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val inactiveColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "nav_icon_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_scale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 28.dp), // Soft unclipped ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier
                        .size(22.dp) // Smaller icon
                        .scale(scale)
                )

                // iOS-style tiny notification badge
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-2).dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 9.sp, // Smaller text
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = iconColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}
