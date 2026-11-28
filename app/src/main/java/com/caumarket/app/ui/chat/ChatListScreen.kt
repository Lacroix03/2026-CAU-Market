package com.caumarket.app.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.caumarket.app.core.model.ChatRoom
import com.caumarket.app.core.model.ChatRoomInfo
import com.caumarket.app.ui.common.formatSmartDate
import com.caumarket.app.ui.common.formatUnreadBadge
import com.caumarket.app.ui.components.EmptyView
import com.caumarket.app.ui.components.ErrorRetryView
import com.caumarket.app.ui.components.SkeletonListLoadingView
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.DangerRed
import com.caumarket.app.ui.theme.SoftGrayBg
import com.caumarket.app.ui.theme.TextPrimary
import com.caumarket.app.ui.theme.TextSecondary
import com.caumarket.app.ui.theme.UnreadRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatClick: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var roomToLeave by remember { mutableStateOf<ChatRoom?>(null) }

    roomToLeave?.let { room ->
        Dialog(onDismissRequest = { roomToLeave = null }) {
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
                            onClick = { roomToLeave = null },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text("취소", color = TextPrimary) }
                        Button(
                            onClick = { viewModel.leaveChatRoom(room.chatId); roomToLeave = null },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) { Text("나가기", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("채팅", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = BorderGray)
            }
        },
        bottomBar = bottomBar
    ) { padding ->
        when {
            state.isLoading -> SkeletonListLoadingView(modifier = Modifier.padding(padding))
            state.errorMessage != null -> ErrorRetryView(
                message = state.errorMessage!!,
                onRetry = viewModel::startObserving,
                modifier = Modifier.padding(padding)
            )
            state.rooms.isEmpty() -> EmptyView(
                title = "진행 중인 채팅이 없어요",
                subtitle = "관심 있는 물품에서 '채팅하기'를 눌러\n대화를 시작해 보세요.",
                icon = Icons.Outlined.ChatBubbleOutline,
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(state.rooms, key = { it.chatId }) { room ->
                        FigmaChatListRow(
                            room = room,
                            info = state.roomInfos[room.chatId],
                            currentUserId = state.currentUserId,
                            onClick = { onChatClick(room.chatId) },
                            onLongClick = { roomToLeave = room }
                        )
                        HorizontalDivider(color = BorderGray, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FigmaChatListRow(
    room: ChatRoom,
    info: ChatRoomInfo?,
    currentUserId: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val unreadBadge = formatUnreadBadge(room.unreadCount[currentUserId] ?: 0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftGrayBg))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info?.opponentNickname ?: "상대방",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatSmartDate(room.lastMessageAt ?: room.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.lastMessage ?: "대화를 시작해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unreadBadge != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = UnreadRed, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = unreadBadge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        AsyncImage(
            model = info?.product?.imageUrls?.firstOrNull(),
            contentDescription = info?.product?.title ?: "삭제된 물품",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(SoftGrayBg)
        )
    }
}
