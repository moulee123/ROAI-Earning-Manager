package com.roaimoneymanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roaimoneymanager.data.local.entity.TransactionEntity
import com.roaimoneymanager.domain.model.TransactionStatus
import com.roaimoneymanager.domain.model.TransactionType
import com.roaimoneymanager.ui.theme.*
import com.roaimoneymanager.util.DateUtils
import com.roaimoneymanager.util.MoneyUtils

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Category Icon Badge
            val icon = getCategoryIcon(transaction.category)
            val iconBg = when (transaction.type) {
                TransactionType.INCOME -> MaterialTheme.colorScheme.primaryContainer
                TransactionType.EXPENSE -> Color(0xFFFDEBD0).copy(alpha = 0.8f)
                TransactionType.TRANSFER -> Color(0xFFE8F8F5).copy(alpha = 0.8f)
            }
            val iconTint = when (transaction.type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // CENTER: Details
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                // Meta Row: Category (Ellipsized) • Payment Method (Single line) • Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category Text (Flexible weight, ellipsizes long names)
                    Text(
                        text = transaction.category,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )

                    // Payment Method (Guaranteed single line, never wraps vertically)
                    Text(
                        text = transaction.paymentMethod.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (transaction.status == TransactionStatus.PENDING) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningYellow.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PENDING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningYellow,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(Modifier.height(3.dp))

                // Date
                Text(
                    text = DateUtils.formatDateTime(transaction.dateMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(Modifier.width(10.dp))

            // RIGHT: Amount & Delete Action
            val prefix = when (transaction.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "−"
                TransactionType.TRANSFER -> "⇄"
            }
            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$prefix${MoneyUtils.formatPaiseToRupees(transaction.amountPaise)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(Modifier.height(2.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food", "restaurant" -> Icons.Default.Restaurant
        "petrol", "fuel" -> Icons.Default.LocalGasStation
        "transport", "bus", "travel" -> Icons.Default.DirectionsBus
        "college", "education" -> Icons.Default.School
        "shopping" -> Icons.Default.ShoppingBag
        "bills", "recharge" -> Icons.Default.Receipt
        "entertainment" -> Icons.Default.Movie
        "salary" -> Icons.Default.Payments
        "work income" -> Icons.Default.WorkHistory
        "freelance" -> Icons.Default.Laptop
        "pocket money" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Category
    }
}
