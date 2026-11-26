package com.cau.campusmarket.ui.product

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cau.campusmarket.di.AppConfig
import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ProductCategory
import com.cau.campusmarket.domain.model.ProductCondition
import com.cau.campusmarket.domain.model.TradeStatus
import com.cau.campusmarket.domain.validation.Validators
import com.cau.campusmarket.ui.components.FigmaErrorBanner
import com.cau.campusmarket.ui.components.FigmaPillChip
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.components.SkeletonListLoadingView
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.SelectedChipBg
import com.cau.campusmarket.ui.theme.SoftGrayBg
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextPrimary
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 4-1 ~ 4-4 물품 등록·수정 화면
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    viewModel: ProductFormViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }
    var showSoldConfirm by remember { mutableStateOf(false) }

    val handleBack = {
        if (state.isDirty && !state.isSubmitting) showExitConfirm = true
        else onBack()
    }

    BackHandler(enabled = state.isDirty && !state.isSubmitting) {
        showExitConfirm = true
    }

    // Figma 4-3 나가기 확인 모달
    if (showExitConfirm) {
        Dialog(onDismissRequest = { showExitConfirm = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("작성을 그만둘까요?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "지금 나가면 작성 중인 내용이 저장되지 않아요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showExitConfirm = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("계속 작성", color = TextPrimary)
                        }
                        FigmaPrimaryButton(
                            text = "나가기",
                            onClick = { showExitConfirm = false; onBack() },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                    }
                }
            }
        }
    }

    // GitHub README 정책: 거래 완료(SOLD)로 변경 시에만 확인창 표시
    if (showSoldConfirm) {
        Dialog(onDismissRequest = { showSoldConfirm = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("거래 완료로 변경할까요?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "거래 완료로 변경하면 기본 물품 목록에서 제외되고 신규 문의가 제한돼요. (기존 채팅은 유지되며 언제든 복구할 수 있어요.)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showSoldConfirm = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("취소", color = TextPrimary)
                        }
                        FigmaPrimaryButton(
                            text = "변경하기",
                            onClick = {
                                showSoldConfirm = false
                                viewModel.onStatusChange(TradeStatus.SOLD)
                            },
                            modifier = Modifier.weight(1f).height(46.dp)
                        )
                    }
                }
            }
        }
    }

    val remaining = (Validators.MAX_PRODUCT_PHOTOS - state.imageUris.size).coerceAtLeast(1)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = Validators.MAX_PRODUCT_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addImages(uris.take(remaining).map { it.toString() })
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (state.isEditMode) "물품 수정" else "물품 등록", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "뒤로 가기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = BorderGray)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                HorizontalDivider(color = BorderGray)
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    FigmaPrimaryButton(
                        text = when {
                            state.isSubmitting && state.isEditMode -> "저장 중…"
                            state.isSubmitting -> "등록 중…"
                            state.isEditMode -> "저장하기"
                            else -> "등록하기"
                        },
                        onClick = { viewModel.submit(onSaved) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        if (state.isLoadingInitial) {
            SkeletonListLoadingView(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // 사진 영역
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("사진", style = MaterialTheme.typography.titleMedium)
                        Text("필수 1장 이상 · 최대 15장", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                    if (!AppConfig.USE_FIREBASE && state.imageUris.size < Validators.MAX_PRODUCT_PHOTOS) {
                        TextButton(onClick = viewModel::addDemoPhoto) {
                            Text("+ 샘플 사진", color = PrimaryBlue)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                                .clickable(enabled = state.imageUris.size < Validators.MAX_PRODUCT_PHOTOS) {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = "사진 추가", tint = TextSecondary)
                                Text(
                                    text = "${state.imageUris.size}/${Validators.MAX_PRODUCT_PHOTOS}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    itemsIndexed(state.imageUris) { idx, uri ->
                        Box(modifier = Modifier.size(76.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "사진 ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SoftGrayBg)
                            )
                            if (idx == 0) {
                                Surface(
                                    color = SelectedChipBg.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "대표",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SelectedChipBg)
                                    .clickable { viewModel.removeImage(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "제거", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Figma 4-2: 수정 화면에서만 3단 거래 상태 세그먼트 표시
            if (state.isEditMode) {
                Column {
                    Text("거래 상태", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = SoftGrayBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            TradeStatus.entries.forEach { st ->
                                val selected = state.status == st
                                Surface(
                                    onClick = {
                                        if (st == TradeStatus.SOLD && state.status != TradeStatus.SOLD) {
                                            showSoldConfirm = true
                                        } else {
                                            viewModel.onStatusChange(st)
                                        }
                                    },
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = if (selected) 1.dp else 0.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = st.displayName,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) TextPrimary else TextSecondary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 물품명
            Column {
                Text("물품명", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    placeholder = { Text("예) 일반물리학 10판", color = TextHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text("${state.title.length} / 50자", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }

            // 카테고리
            Column {
                Text("카테고리", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCategory.entries.forEach { cat ->
                        FigmaPillChip(
                            label = cat.displayName,
                            selected = state.category == cat,
                            onClick = { viewModel.onCategoryChange(cat) }
                        )
                    }
                }
            }

            // 가격 + 무료 나눔
            Column {
                Text("가격", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (state.isFreeShare) "0" else state.priceText,
                    onValueChange = viewModel::onPriceChange,
                    placeholder = { Text("숫자만 입력 (원)", color = TextHint) },
                    enabled = !state.isFreeShare,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.onFreeShareToggle(!state.isFreeShare) }
                ) {
                    Checkbox(
                        checked = state.isFreeShare,
                        onCheckedChange = viewModel::onFreeShareToggle,
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Text("무료 나눔 (선택 시 가격 0원 · '나눔' 표시)", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 물품 상태 (Figma Page 6: 새 상품 / 사용감 적음 / 사용감 많음 / 하자 있음)
            Column {
                Text("물품 상태", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductCondition.entries.forEach { cond ->
                        FigmaPillChip(
                            label = cond.displayName,
                            selected = state.condition == cond,
                            onClick = { viewModel.onConditionChange(cond) }
                        )
                    }
                }
            }

            // 상세 설명
            Column {
                Text("상세 설명", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = { Text("사용 기간, 하자, 구성품, 거래 관련 내용을 적어 주세요.", color = TextHint) },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.description.length} / 1,000자",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            // 거래 희망 위치 (README place 열거형 + placeDetail 선택 입력)
            Column {
                Text("거래 희망 위치", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusPlace.entries.forEach { place ->
                        FigmaPillChip(
                            label = place.displayName,
                            selected = state.place == place,
                            onClick = { viewModel.onPlaceChange(place) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.placeDetail,
                    onValueChange = viewModel::onPlaceDetailChange,
                    placeholder = { Text("상세 위치 (선택, 예: 정문 오른쪽 벤치 · 최대 30자)", color = TextHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 처분 희망 기한 (선택)
            Column {
                Text("처분 희망 기한 (선택)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.disposeByText,
                    onValueChange = viewModel::onDisposeByChange,
                    placeholder = { Text("날짜 입력 (예: 2026-12-20)", color = TextHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.errorMessage?.let { err ->
                FigmaErrorBanner(message = err)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
