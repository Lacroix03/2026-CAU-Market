package com.caumarket.app.ui.chat

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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.caumarket.app.core.model.Message
import com.caumarket.app.ui.common.formatDateDivider
import com.caumarket.app.ui.common.formatPrice
import com.caumarket.app.ui.common.formatTimeOnly
import com.caumarket.app.ui.components.FigmaPrimaryButton
import com.caumarket.app.ui.components.FullscreenImageDialog
import com.caumarket.app.ui.components.TradeStatusBadge
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.DangerRed
import com.caumarket.app.ui.theme.DisabledButtonBg
import com.caumarket.app.ui.theme.PrimaryBlue
import com.caumarket.app.ui.theme.SelectedChipBg
import com.caumarket.app.ui.theme.SoftGrayBg
import com.caumarket.app.ui.theme.TextHint
import com.caumarket.app.ui.theme.TextPrimary
import com.caumarket.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    viewModel: ChatRoomViewModel,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var zoomedImageModel by remember { mutableStateOf<Any?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    zoomedImageModel?.let { model ->
        FullscreenImageDialog(imageModel = model, onDismiss = { zoomedImageModel = null })
    }

    if (showMenuSheet) {
        ModalBottomSheet(onDismissRequest = { showMenuSheet = false }, containerColor = Color.White) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showMenuSheet = false; showLeaveDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("채팅방 나가기", color = DangerRed, fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = { showMenuSheet = false },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderGray),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("취소", color = TextPrimary) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

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
                        ) { Text("취소", color = TextPrimary) }
                        Button(
                            onClick = { showLeaveDialog = false; viewModel.leaveChatRoom(onBack) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text("나가기", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.setPendingImages(uris)
    }

    val info = state.roomInfo
    val product = info?.product

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = info?.opponentNickname ?: "채팅방",
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (info != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = product != null) { product?.let { onProductClick(it.productId) } }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = product?.imageUrls?.firstOrNull(),
                        contentDescription = product?.title ?: "삭제된 물품",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(SoftGrayBg)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (product == null) {
                            Text("삭제된 물품", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TradeStatusBadge(statusCode = product.status)
                                Text(
                                    text = product.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(formatPrice(product.price), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = BorderGray)
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                itemsIndexed(state.messages, key = { _, m -> m.messageId }) { index, msg ->
                    val curDate = formatDateDivider(msg.createdAt)
                    val prevDate = if (index > 0) formatDateDivider(state.messages[index - 1].createdAt) else null
                    if (curDate != prevDate) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Surface(color = SoftGrayBg, shape = RoundedCornerShape(12.dp)) {
                                Text(curDate, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }
                    }

                    if (msg.type == "SYSTEM") {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Surface(color = SoftGrayBg, shape = RoundedCornerShape(12.dp)) {
                                Text(msg.text.orEmpty(), style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                            }
                        }
                    } else {
                        val isMine = msg.senderId == state.currentUserId
                        // 백엔드 규격: 내 TEXT/IMAGE 메시지의 sequence가 상대 lastReadSequence보다 크면 '1'
                        val isUnread = isMine && msg.sequence > state.opponentLastReadSequence
                        FigmaMessageBubble(
                            message = msg,
                            isMine = isMine,
                            isUnread = isUnread,
                            imageBytesCache = state.imageBytesCache,
                            onImageClick = { zoomedImageModel = it }
                        )
                    }
                }
            }

            state.errorMessage?.let { err ->
                Text(err, color = DangerRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (state.pendingImages.isNotEmpty()) {
                HorizontalDivider(color = BorderGray)
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("사진 선택", style = MaterialTheme.typography.titleMedium)
                        Text("${state.pendingImages.size} / 10", style = MaterialTheme.typography.labelMedium, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(state.pendingImages) { idx, uri ->
                            Box(modifier = Modifier.size(68.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "선택 사진 ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
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
                        text = if (state.isSending) "전송 중…" else "${state.pendingImages.size}장 전송",
                        onClick = viewModel::sendPendingImages,
                        enabled = !state.isSending,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = BorderGray)
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = !state.isOpponentLeft && !state.isSending,
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "사진 첨부", tint = if (state.isOpponentLeft) TextHint else TextSecondary)
                }

                OutlinedTextField(
                    value = state.messageInput,
                    onValueChange = viewModel::onInputChange,
                    enabled = !state.isOpponentLeft,
                    placeholder = {
                        Text(if (state.isOpponentLeft) "상대방이 채팅방을 나갔습니다." else "메시지 입력", color = TextHint)
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = Color.White, modifier = Modifier.size(18.dp))
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
    isUnread: Boolean,
    imageBytesCache: Map<String, ByteArray>,
    onImageClick: (Any) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMine) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    text = if (isUnread) "1" else "읽음",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUnread) PrimaryBlue else TextSecondary,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
                Text(formatTimeOnly(message.createdAt), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }

        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 260.dp)) {
            if (message.type == "IMAGE" && message.imageUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(message.imageUrls) { idx, _ ->
                        val bytes = imageBytesCache["${message.messageId}_$idx"]
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftGrayBg)
                                .clickable(enabled = bytes != null) { bytes?.let(onImageClick) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bytes != null) {
                                AsyncImage(
                                    model = bytes,
                                    contentDescription = "채팅 사진",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
            if (!message.text.isNullOrBlank()) {
                Surface(color = if (isMine) PrimaryBlue else SoftGrayBg, shape = RoundedCornerShape(16.dp)) {
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
            Text(formatTimeOnly(message.createdAt), style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
