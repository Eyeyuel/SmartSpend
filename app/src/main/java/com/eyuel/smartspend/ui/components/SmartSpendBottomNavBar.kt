package com.eyuel.smartspend.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern sleek Bottom Navigation Bar for SmartSpend.
 *
 * Features:
 * - Floating centered pill design inspired by Telegram's new UI.
 * - Deep dark surface (#131C29) with rounded corners and shadow.
 * - Vibrant electric cyan/blue (#2AABEE) active tint and muted slate (#8E9CAE) inactive tint.
 * - Flat icon & label design without top indicators.
 * - Micro-spring scale animations and color transitions on selection.
 * - Unreviewed transaction badge counter.
 */
@Composable
fun SmartSpendBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    unreviewedCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val barBg = Color(0xFF131C29)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = barBg,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.height(64.dp).fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    title = "Ledger",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    isSelected = selectedTab == 0,
                    badgeCount = unreviewedCount,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                NavBarItem(
                    title = "Insights",
                    icon = Icons.Default.Insights,
                    isSelected = selectedTab == 1,
                    badgeCount = 0,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
    val activeColor = Color(0xFF2AABEE)
    val inactiveColor = Color(0xFF8E9CAE)

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 180),
        label = "nav_icon_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_scale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
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
                        .size(24.dp)
                        .scale(scale)
                )

                // Notification counter badge
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 14.dp, y = (-4).dp)
                            .height(16.dp)
                            .widthIn(min = 16.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = iconColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}
