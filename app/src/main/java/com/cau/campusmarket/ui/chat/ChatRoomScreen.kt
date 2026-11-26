package com.cau.campusmarket.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cau.campusmarket.domain.model.MeetupSchedule
import com.cau.campusmarket.domain.model.MeetupStatus
import com.cau.campusmarket.domain.model.Message
import com.cau.campusmarket.domain.model.MessageType
import com.cau.campusmarket.domain.validation.Validators
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.components.FullscreenImageDialog
import com.cau.campusmarket.ui.components.TradeStatusBadge
import com.cau.campusmarket.ui.components.formatDateDivider
import com.cau.campusmarket.ui.components.formatPrice
import com.cau.campusmarket.ui.components.formatTimeOnly
import com.cau.campusmarket.ui.theme.BadgeConfirmedBg
import com.cau.campusmarket.ui.theme.BadgeConfirmedFg
import com.cau.campusmarket.ui.theme.BadgeReservedBg
import com.cau.campusmarket.ui.theme.BadgeReservedFg
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.DangerRed
import com.cau.campusmarket.ui.theme.DisabledButtonBg
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.PrimaryBlueBorder
import com.cau.campusmarket.ui.theme.PrimaryBlueSoftBg
import com.cau.campusmarket.ui.theme.SelectedChipBg
import com.cau.campusmarket.ui.theme.SoftGrayBg
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextPrimary
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 6-1 ~ 6-3 채팅방 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatRoomViewModel,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onOpenMeetup: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        viewModel.markAsRead()
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    zoomedImageUrl?.let { url ->
        FullscreenImageDialog(imageUrl = url, onDismiss = { zoomedImageUrl = null })
    }

    // Figma 6-3 상단 ⋯ 메뉴 바텀시트
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.room?.let { room ->
                    OutlinedButton(
                        onClick = {
                            showMenuSheet = false
                            onOpenMeetup(room.chatId)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryBlueBorder),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("거래 일정 협의", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(
                    onClick = {
                        showMenuSheet = false
                        showLeaveDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("채팅방 나가기", color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showMenuSheet = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("취소", color = TextPrimary)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // 채팅방 나가기 확인 모달
    if (showLeaveDialog) {
        Dialog(onDismissRequest = { showLeaveDialog = false }) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("채팅방을 나갈까요?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "나가면 내 채팅 목록과 대화 화면에서 사라지고 새 메시지를 받을 수 없어요. 상대방의 대화 기록은 유지돼요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showLeaveDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("취소", color = TextPrimary)
                        }
                        Button(
                            onClick = {
                                showLeaveDialog = false
                                viewModel.leaveChatRoom(onBack)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("나가기", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = Validators.MAX_CHAT_PHOTOS)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.setPendingImages(uris.map { it.toString() })
    }

    val room = state.room

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = room?.opponentNickname(state.currentUserId) ?: "채팅방",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "뒤로 가기")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenuSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "채팅방 메뉴")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = BorderGray)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Figma 6-1 상단 물품 요약 + 우측 '📅 거래 일정' 버튼
            if (room != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !room.productDeleted) { onProductClick(room.productId) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = room.productThumbnailUrl,
                        contentDescription = room.productTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftGrayBg)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (room.productDeleted) {
                            Text(
                                text = "삭제된 물품",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TradeStatusBadge(status = room.productStatus)
                                Text(
                                    text = room.productTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formatPrice(room.productPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { onOpenMeetup(room.chatId) },
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlueSoftBg,
                        border = BorderStroke(1.dp, PrimaryBlueBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "거래 일정",
                                color = PrimaryBlue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                HorizontalDivider(color = BorderGray)
            }

            // 메시지 목록 + 거래 일정 카드
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(state.messages, key = { _, m -> m.messageId }) { index, msg ->
                    val curDate = formatDateDivider(msg.createdAt)
                    val prevDate = if (index > 0) formatDateDivider(state.messages[index - 1].createdAt) else null
                    if (curDate != prevDate) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(color = SoftGrayBg, shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    text = curDate,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (msg.type == MessageType.SYSTEM) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(color = SoftGrayBg, shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    text = msg.text.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    } else {
                        val isMine = msg.senderId == state.currentUserId
                        val opponentLastRead = room?.opponentLastReadAt(state.currentUserId) ?: 0L
                        val isRead = opponentLastRead >= msg.createdAt
                        FigmaMessageBubble(
                            message = msg,
                            isMine = isMine,
                            isRead = isRead,
                            onImageClick = { zoomedImageUrl = it }
                        )
                    }
                }

                // Figma 6-1 채팅방 내 거래 일정 안내 카드
                state.meetup?.takeIf { it.status != MeetupStatus.CANCELLED }?.let { meetup ->
                    item(key = "meetup_card") {
                        FigmaInChatMeetupCard(
                            meetup = meetup,
                            isProposedByMe = meetup.proposerId == state.currentUserId,
                            onClick = { room?.let { onOpenMeetup(it.chatId) } }
                        )
                    }
                }
            }

            state.errorMessage?.let { err ->
                Text(
                    text = err,
                    color = DangerRed,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Figma 6-2 사진 첨부 미리보기 + 'N장 전송' 버튼
            if (state.pendingImages.isNotEmpty()) {
                HorizontalDivider(color = BorderGray)
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("사진 선택", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${state.pendingImages.size} / ${Validators.MAX_CHAT_PHOTOS}",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.pendingImages) { idx, uri ->
                            Box(modifier = Modifier.size(68.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "선택 사진 ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(SelectedChipBg)
                                        .clickable { viewModel.removePendingImage(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "제거", tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FigmaPrimaryButton(
                        text = "${state.pendingImages.size}장 전송",
                        onClick = viewModel::sendPendingImages,
                        enabled = !state.isSending,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 하단 입력 바 (상대방이 나갔으면 입력창 비활성화 — README 정책)
            HorizontalDivider(color = BorderGray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !state.isOpponentLeft && !state.isSending,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = "사진 첨부",
                        tint = if (state.isOpponentLeft) TextHint else TextSecondary
                    )
                }

                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = viewModel::onInputChange,
                    enabled = !state.isOpponentLeft,
                    placeholder = {
                        Text(
                            text = if (state.isOpponentLeft) "상대방이 채팅방을 나갔습니다." else "메시지 입력",
                            color = TextHint
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SoftGrayBg,
                        focusedContainerColor = SoftGrayBg,
                        disabledContainerColor = SoftGrayBg,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = PrimaryBlue,
                        disabledBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                val canSend = !state.isOpponentLeft && state.messageInput.isNotBlank() && !state.isSending
                Surface(
                    onClick = viewModel::sendMessage,
                    enabled = canSend,
                    shape = CircleShape,
                    color = if (canSend) PrimaryBlue else DisabledButtonBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "전송",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaMessageBubble(
    message: Message,
    isMine: Boolean,
    isRead: Boolean,
    onImageClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMine) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = if (isRead) "읽음" else "1",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRead) TextSecondary else PrimaryBlue,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = formatTimeOnly(message.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            if (message.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(message.imageUrls) { _, url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "채팅 사진",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(132.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGrayBg)
                                .clickable { onImageClick(url) }
                        )
                    }
                }
            }
            if (!message.text.isNullOrBlank()) {
                Surface(
                    color = if (isMine) PrimaryBlue else SoftGrayBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = message.text,
                        color = if (isMine) Color.White else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        if (!isMine) {
            Text(
                text = formatTimeOnly(message.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun FigmaInChatMeetupCard(
    meetup: MeetupSchedule,
    isProposedByMe: Boolean,
    onClick: () -> Unit
) {
    val isConfirmed = meetup.status == MeetupStatus.CONFIRMED
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryBlueBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isConfirmed) "확정된 거래 일정" else "거래 일정 제안",
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = if (isConfirmed) BadgeConfirmedBg else BadgeReservedBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = meetup.status.displayName,
                        color = if (isConfirmed) BadgeConfirmedFg else BadgeReservedFg,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(meetup.formattedLocation(), style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("${meetup.dateText} ${meetup.timeText}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "제안: ${if (isProposedByMe) "나" else meetup.proposerNickname}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}
