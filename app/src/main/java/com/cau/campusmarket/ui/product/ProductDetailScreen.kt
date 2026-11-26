package com.cau.campusmarket.ui.product

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.ui.components.ErrorRetryView
import com.cau.campusmarket.ui.components.FigmaErrorBanner
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.components.FullscreenImageDialog
import com.cau.campusmarket.ui.components.SkeletonListLoadingView
import com.cau.campusmarket.ui.components.TradeStatusBadge
import com.cau.campusmarket.ui.components.formatDisposeDate
import com.cau.campusmarket.ui.components.formatPrice
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.DangerRed
import com.cau.campusmarket.ui.theme.InfoCardBg
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.SoftGrayBg
import com.cau.campusmarket.ui.theme.TextPrimary
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 3-1 ~ 3-4 물품 상세 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleted: () -> Unit,
    onOpenChat: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProduct()
    }

    zoomedImageUrl?.let { url ->
        FullscreenImageDialog(imageUrl = url, onDismiss = { zoomedImageUrl = null })
    }

    // Figma 3-4 삭제 확인창
    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("물품을 삭제할까요?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "삭제한 물품은 복구할 수 없어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("취소", color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                viewModel.deleteProduct(onDeleted)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("삭제", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    val product = state.product

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    if (state.isOwner) {
                        Text(
                            text = "내 물품",
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (product != null) {
                Column(modifier = Modifier.background(Color.White)) {
                    HorizontalDivider(color = BorderGray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.isOwner) {
                            // Figma 3-2: 수정 / 삭제 나란히 배치
                            OutlinedButton(
                                onClick = { onEditClick(product.productId) },
                                enabled = !state.isSubmittingAction,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("수정", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = !state.isSubmittingAction,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text("삭제", color = DangerRed, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            // Figma 3-1 & 3-3: 판매 중·예약 중 -> 채팅하기 / 거래 완료 -> 비활성화
                            val canInquire = product.status != TradeStatus.SOLD
                            FigmaPrimaryButton(
                                text = if (canInquire) "채팅하기" else "거래 완료된 물품이에요",
                                onClick = { viewModel.startChat(onOpenChat) },
                                enabled = canInquire && !state.isSubmittingAction,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> SkeletonListLoadingView(modifier = Modifier.padding(padding))
            product == null -> ErrorRetryView(
                message = state.errorMessage ?: "불러오지 못했어요",
                onRetry = viewModel::loadProduct,
                modifier = Modifier.padding(padding)
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 사진 페이저
                    val images = product.imageUrls
                    if (images.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { images.size })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .background(SoftGrayBg)
                        ) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                AsyncImage(
                                    model = images[page],
                                    contentDescription = "${product.title} 사진 ${page + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { zoomedImageUrl = images[page] }
                                )
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "사진 ${pagerState.currentPage + 1} / ${images.size} · 탭하면 확대",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 판매자 정보 행
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SoftGrayBg)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (state.isOwner) "나 (판매자)" else product.sellerNickname,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    HorizontalDivider(color = BorderGray)

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TradeStatusBadge(status = product.status)
                            Text(
                                text = "${product.category.displayName} · ${product.condition.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(text = product.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = formatPrice(product.price),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        if (!product.description.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(text = product.description, style = MaterialTheme.typography.bodyLarge)
                        }

                        Spacer(Modifier.height(20.dp))

                        // Figma 연회색 정보 박스 (거래 희망 위치 & 처분 희망 기한)
                        Surface(
                            color = InfoCardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("거래 희망 위치", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = product.formattedPlace(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                formatDisposeDate(product.disposeBy)?.let { disposeText ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Text("처분 희망 기한", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = disposeText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        state.errorMessage?.let { err ->
                            Spacer(Modifier.height(14.dp))
                            FigmaErrorBanner(message = err)
                        }
                    }
                }
            }
        }
    }
}
