package com.caumarket.app.ui.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.caumarket.app.core.model.Product
import com.caumarket.app.ui.common.formatPrice
import com.caumarket.app.ui.components.EmptyView
import com.caumarket.app.ui.components.ErrorRetryView
import com.caumarket.app.ui.components.FigmaPrimaryButton
import com.caumarket.app.ui.components.SkeletonListLoadingView
import com.caumarket.app.ui.components.TradeStatusBadge
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.PrimaryBlue
import com.caumarket.app.ui.theme.PrimaryBlueSoftBg
import com.caumarket.app.ui.theme.SoftGrayBg
import com.caumarket.app.ui.theme.TextPrimary
import com.caumarket.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel,
    onProductClick: (String) -> Unit,
    onEditProductClick: (String) -> Unit,
    onCreateProductClick: () -> Unit,
    onLogoutClick: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMyProducts()
    }

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("로그아웃할까요?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("다시 로그인해야 거래 기능을 이용할 수 있어요.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text("취소", color = TextPrimary) }
                        FigmaPrimaryButton(
                            text = "로그아웃",
                            onClick = { showLogoutDialog = false; onLogoutClick() },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                    }
                }
            }
        }
    }

    val tabs: List<Pair<String, String?>> = listOf(
        "전체 ${state.totalCount}" to null,
        "판매 중 ${state.sellingCount}" to "SELLING",
        "예약 중 ${state.reservedCount}" to "RESERVED",
        "거래 완료 ${state.soldCount}" to "SOLD"
    )
    val selectedIndex = tabs.indexOfFirst { it.second == state.selectedStatus }.coerceAtLeast(0)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("마이페이지", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = BorderGray)
            }
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftGrayBg))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.user?.nickname ?: "사용자", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(state.user?.email.orEmpty(), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("로그아웃", style = MaterialTheme.typography.labelMedium, color = TextSecondary) }
            }

            HorizontalDivider(thickness = 8.dp, color = SoftGrayBg)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("내 판매 물품", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(onClick = onCreateProductClick, color = PrimaryBlueSoftBg, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "+ 등록",
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = Color.White,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = TextPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { idx, (label, status) ->
                    Tab(
                        selected = selectedIndex == idx,
                        onClick = { viewModel.selectStatusTab(status) },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedIndex == idx) TextPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            when {
                state.isLoading -> SkeletonListLoadingView()
                state.errorMessage != null -> ErrorRetryView(message = state.errorMessage!!, onRetry = viewModel::loadMyProducts)
                state.filteredProducts.isEmpty() -> EmptyView(
                    title = "아직 등록한 물품이 없어요",
                    subtitle = "첫 물품을 등록해 보세요.",
                    actionLabel = "물품 등록하기",
                    onAction = onCreateProductClick
                )
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredProducts, key = { it.productId }) { product ->
                            FigmaMyProductRow(
                                product = product,
                                onClick = { onProductClick(product.productId) },
                                onEditClick = { onEditProductClick(product.productId) }
                            )
                            HorizontalDivider(color = BorderGray, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaMyProductRow(product: Product, onClick: () -> Unit, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = product.imageUrls.firstOrNull(),
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(8.dp)).background(SoftGrayBg)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(formatPrice(product.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            TradeStatusBadge(statusCode = product.status)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onEditClick,
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Text("수정", style = MaterialTheme.typography.labelMedium, color = TextPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}
