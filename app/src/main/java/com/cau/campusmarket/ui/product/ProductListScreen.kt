package com.cau.campusmarket.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cau.campusmarket.domain.model.Product
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.ui.components.EmptyView
import com.cau.campusmarket.ui.components.ErrorRetryView
import com.cau.campusmarket.ui.components.FigmaPillChip
import com.cau.campusmarket.ui.components.SkeletonListLoadingView
import com.cau.campusmarket.ui.components.TradeStatusBadge
import com.cau.campusmarket.ui.components.formatPrice
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.SoftGrayBg
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 2-1 / 2-2 / 2-3 물품 목록 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    onProductClick: (String) -> Unit,
    onCreateProductClick: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면 복귀 시 최신 목록 갱신
    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("중앙대 캠퍼스 마켓", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::loadProducts) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            // Figma 파란 알약형 '+ 등록' 버튼
            ExtendedFloatingActionButton(
                onClick = onCreateProductClick,
                shape = RoundedCornerShape(24.dp),
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("등록", fontWeight = FontWeight.SemiBold) }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Figma 연회색 둥근 검색창
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("제목·설명으로 검색", color = TextHint) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "검색어 지우기", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = SoftGrayBg,
                    focusedContainerColor = SoftGrayBg,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = PrimaryBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 카테고리 필터 칩 (전체·교재·생활용품·전자기기·의류·기타)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FigmaPillChip(
                        label = "전체",
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) }
                    )
                }
                items(ProductCategory.entries) { cat ->
                    FigmaPillChip(
                        label = cat.displayName,
                        selected = state.selectedCategory == cat,
                        onClick = { viewModel.onCategorySelected(cat) }
                    )
                }
            }

            // Figma 2-2: 검색 결과 화면에서만 건수 + '거래 완료 제외' 체크박스 표시
            if (state.isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "검색 결과 ${state.products.size}건",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.toggleExcludeSoldInSearch() }
                    ) {
                        Checkbox(
                            checked = state.excludeSoldInSearch,
                            onCheckedChange = { viewModel.toggleExcludeSoldInSearch() },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                        )
                        Text("거래 완료 제외", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = BorderGray)

            when {
                state.isLoading -> SkeletonListLoadingView()
                state.errorMessage != null -> ErrorRetryView(
                    message = state.errorMessage!!,
                    onRetry = viewModel::loadProducts
                )
                state.products.isEmpty() -> {
                    if (state.isSearching) {
                        EmptyView(
                            title = "검색 결과가 없어요",
                            subtitle = "다른 검색어를 입력하거나 카테고리를 바꿔 보세요.",
                            icon = Icons.Outlined.SearchOff
                        )
                    } else {
                        EmptyView(
                            title = "아직 등록된 물품이 없어요",
                            subtitle = "첫 물품을 등록해 보세요.",
                            actionLabel = "물품 등록하기",
                            onAction = onCreateProductClick
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.products, key = { it.productId }) { product ->
                            FigmaProductListRow(
                                product = product,
                                onClick = { onProductClick(product.productId) }
                            )
                            HorizontalDivider(
                                color = BorderGray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

/**
 * Figma Page 1 & 4 물품 목록 행 (좌측 썸네일 / 우측 제목 → 가격 → 상태 배지)
 */
@Composable
private fun FigmaProductListRow(
    product: Product,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = product.thumbnailUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoftGrayBg)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatPrice(product.price),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            TradeStatusBadge(status = product.status)
        }
    }
}
