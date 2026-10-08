package com.eyuel.smartspend.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuel.smartspend.data.local.TransactionEntity
import com.eyuel.smartspend.domain.model.ExpenseCategory
import com.eyuel.smartspend.domain.model.TransactionType
import com.eyuel.smartspend.ui.CategoryVisuals
import com.eyuel.smartspend.ui.FormatUtils

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInternalTransfer = transaction.category == ExpenseCategory.INTERNAL_TRANSFER
    val isIncome = transaction.type == TransactionType.CREDIT && !isInternalTransfer
    val categoryStyle = remember(transaction.category) {
        CategoryVisuals.getStyle(transaction.category)
    }
    val bankVisual = remember(transaction.bankName) {
        CategoryVisuals.getBankVisual(transaction.bankName)
    }
    val formattedDate = remember(transaction.timestamp) {
        FormatUtils.formatShortDate(transaction.timestamp)
    }
    val formattedAmount = remember(transaction.amount, isIncome, isInternalTransfer, transaction.currency) {
        when {
            isInternalTransfer -> "${FormatUtils.formatAmount(transaction.amount)}"
            isIncome -> "+${FormatUtils.formatAmount(transaction.amount)}"
            else -> "-${FormatUtils.formatAmount(transaction.amount)}"
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp) // Less vertical padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp) // Slightly smaller icon badge
                    .clip(CircleShape)
                    .background(categoryStyle.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(20.dp)
                )

                // Unreviewed amber indicator dot
                if (!transaction.isReviewed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp)) // Less spacing

            // Center details: Title & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.description.isNotBlank()) transaction.description else transaction.category,
                    fontSize = 14.sp, // Smaller title
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${bankVisual.shortName} • $formattedDate",
                    fontSize = 12.sp, // Smaller subtitle
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right column: Amount
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedAmount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, // Smaller amount
                    color = when {
                        isInternalTransfer -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        isIncome -> Color(0xFF10B981) // Emerald 500
                        else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (!transaction.isReviewed) {
                    Text(
                        text = "Review",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp, // Smaller
                        fontWeight = FontWeight.Medium
                    )
                } else if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.category,
                        fontSize = 10.sp, // Smaller
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
