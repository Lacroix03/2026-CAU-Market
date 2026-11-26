package com.cau.campusmarket.ui.meetup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.MeetupStatus
import com.cau.campusmarket.ui.components.FigmaPillChip
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.theme.BadgeConfirmedBg
import com.cau.campusmarket.ui.theme.BadgeConfirmedFg
import com.cau.campusmarket.ui.theme.BadgeReservedBg
import com.cau.campusmarket.ui.theme.BadgeReservedFg
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.DangerRed
import com.cau.campusmarket.ui.theme.InfoCardBg
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.PrimaryBlueSoftBg
import com.cau.campusmarket.ui.theme.SoftGrayBg
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextPrimary
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 8-1 / 8-2 거래 일정 협의 화면
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MeetupScreen(
    viewModel: MeetupViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("거래 일정 협의", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                        text = "일정 제안하기",
                        onClick = viewModel::proposeSchedule,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 거래 물품 · 상대 정보 카드
            state.room?.let { room ->
                Surface(
                    color = InfoCardBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = room.productThumbnailUrl,
                            contentDescription = room.productTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftGrayBg)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(room.productTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${room.opponentNickname(state.currentUserId)}님과의 거래",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Figma 8-2 제안 확인 · 수락 · 확정 · 취소 카드
            state.schedule?.takeIf { it.status != MeetupStatus.CANCELLED }?.let { schedule ->
                val isConfirmed = schedule.status == MeetupStatus.CONFIRMED
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isConfirmed) "확정된 일정" else "받은 일정 제안",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (isConfirmed) BadgeConfirmedBg else BadgeReservedBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = schedule.status.displayName,
                                    color = if (isConfirmed) BadgeConfirmedFg else BadgeReservedFg,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("장소  ${schedule.formattedLocation()}", style = MaterialTheme.typography.bodyMedium)
                        Text("시간  ${schedule.dateText} ${schedule.timeText}", style = MaterialTheme.typography.bodyMedium)
                        Text("제안자  ${schedule.proposerNickname}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        if (schedule.memo.isNotBlank()) {
                            Text("메모  ${schedule.memo}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }

                        Spacer(Modifier.height(6.dp))
                        if (isConfirmed) {
                            OutlinedButton(
                                onClick = viewModel::cancelSchedule,
                                enabled = !state.isSubmitting,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderGray),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("일정 취소", color = DangerRed, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::cancelSchedule,
                                    enabled = !state.isSubmitting,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderGray),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("취소", color = TextPrimary)
                                }
                                if (schedule.proposerId != state.currentUserId) {
                                    FigmaPrimaryButton(
                                        text = "수락",
                                        onClick = viewModel::acceptSchedule,
                                        enabled = !state.isSubmitting,
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isConfirmed) {
                    Surface(
                        color = PrimaryBlueSoftBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "확정 일정은 양쪽 모두 채팅방 일정 카드에서 확인할 수 있어요.",
                            color = PrimaryBlue,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Figma 8-1 일정 제안 입력 폼
            Column {
                Text("교내 장소", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CampusPlace.entries.forEach { place ->
                        FigmaPillChip(
                            label = place.displayName,
                            selected = state.selectedPlace == place,
                            onClick = { viewModel.onPlaceSelect(place) }
                        )
                    }
                }
            }

            Column {
                Text("장소 상세", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.placeDetail,
                    onValueChange = viewModel::onPlaceDetailChange,
                    placeholder = { Text("예) 정문 오른쪽 벤치", color = TextHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("날짜", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.dateText,
                        onValueChange = viewModel::onDateChange,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(0.9f)) {
                    Text("시간", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.timeText,
                        onValueChange = viewModel::onTimeChange,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column {
                Text("메모", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.memo,
                    onValueChange = viewModel::onMemoChange,
                    placeholder = { Text("요청사항 입력 (예: 도착하면 채팅 주세요)", color = TextHint) },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.feedbackMessage?.let { msg ->
                Text(msg, color = PrimaryBlue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}
