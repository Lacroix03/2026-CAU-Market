package com.caumarket.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.caumarket.app.ui.common.MarketOptions
import com.caumarket.app.ui.theme.BadgeReservedBg
import com.caumarket.app.ui.theme.BadgeReservedFg
import com.caumarket.app.ui.theme.BadgeSellingBg
import com.caumarket.app.ui.theme.BadgeSellingFg
import com.caumarket.app.ui.theme.BadgeSoldBg
import com.caumarket.app.ui.theme.BadgeSoldFg
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.DangerRed
import com.caumarket.app.ui.theme.DisabledButtonBg
import com.caumarket.app.ui.theme.DisabledButtonFg
import com.caumarket.app.ui.theme.ErrorBoxBg
import com.caumarket.app.ui.theme.ErrorBoxFg
import com.caumarket.app.ui.theme.PrimaryBlue
import com.caumarket.app.ui.theme.SelectedChipBg
import com.caumarket.app.ui.theme.SelectedChipFg
import com.caumarket.app.ui.theme.SoftGrayBg
import com.caumarket.app.ui.theme.TextPrimary
import com.caumarket.app.ui.theme.TextSecondary

@Composable
fun TradeStatusBadge(statusCode: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (statusCode) {
        "SELLING" -> BadgeSellingBg to BadgeSellingFg
        "RESERVED" -> BadgeReservedBg to BadgeReservedFg
        else -> BadgeSoldBg to BadgeSoldFg
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = MarketOptions.statusLabel(statusCode),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun FigmaPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) SelectedChipBg else Color.White,
        border = if (selected) null else BorderStroke(1.dp, BorderGray)
    ) {
        Text(
            text = label,
            color = if (selected) SelectedChipFg else TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun FigmaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            disabledContainerColor = DisabledButtonBg,
            disabledContentColor = DisabledButtonFg
        ),
        modifier = modifier.height(50.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FigmaErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(color = ErrorBoxBg, shape = RoundedCornerShape(8.dp), modifier = modifier.fillMaxWidth()) {
        Text(
            text = message,
            color = ErrorBoxFg,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun SkeletonListLoadingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftGrayBg)
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.75f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(SoftGrayBg))
                    Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(SoftGrayBg))
                    Box(Modifier.width(48.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(SoftGrayBg))
                }
            }
        }
    }
}

@Composable
fun EmptyView(
    title: String,
    subtitle: String = "",
    icon: ImageVector = Icons.Outlined.Inventory2,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.55f), modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Text(actionLabel, color = TextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ErrorRetryView(
    message: String = "불러오지 못했어요",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, BorderGray)) {
            Text("다시 시도", color = TextPrimary)
        }
    }
}

@Composable
fun FullscreenImageDialog(
    imageModel: Any,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "확대 이미지",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
            }
        }
    }
}
