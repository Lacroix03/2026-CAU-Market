package com.caumarket.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.error.MarketException
import com.caumarket.app.core.model.ChatRoom
import com.caumarket.app.core.model.ChatRoomInfo
import com.caumarket.app.core.model.Message
import com.caumarket.app.data.repository.AuthRepository
import com.caumarket.app.data.repository.ChatRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatRoomUiState(
    val currentUserId: String = "",
    val room: ChatRoom? = null,
    val roomInfo: ChatRoomInfo? = null,
    val messages: List<Message> = emptyList(),
    val imageBytesCache: Map<String, ByteArray> = emptyMap(),
    val messageInput: String = "",
    val pendingImages: List<Uri> = emptyList(),
    val pendingTextRequestId: String? = null,
    val pendingImageRequestId: String? = null,
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    val opponentId: String
        get() = roomInfo?.opponentId
            ?: room?.let { if (currentUserId == it.buyerId) it.sellerId else it.buyerId }.orEmpty()

    val isOpponentLeft: Boolean
        get() = room != null && opponentId.isNotEmpty() && opponentId !in room.activeUserIds

    val opponentLastReadSequence: Long
        get() = room?.lastReadSequence?.get(opponentId) ?: 0L
}

class ChatRoomViewModel(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private var roomsJob: Job? = null
    private var messagesJob: Job? = null
    private var lastMarkedMessageId: String? = null
    private val loadingImageKeys = mutableSetOf<String>()

    init {
        startObserving()
    }

    fun startObserving() {
        roomsJob?.cancel()
        messagesJob?.cancel()

        viewModelScope.launch {
            val uid = authRepository.getCurrentUser().getOrNull()?.userId.orEmpty()
            _uiState.update { it.copy(currentUserId = uid) }
            chatRepository.getChatRoomInfo(chatId).fold(
                onSuccess = { info -> _uiState.update { it.copy(roomInfo = info) } },
                onFailure = { e -> _uiState.update { it.copy(errorMessage = MarketErrorMapper.toMessage(e)) } }
            )
        }

        roomsJob = viewModelScope.launch {
            chatRepository.observeChatRooms().collect { res ->
                res.onSuccess { list ->
                    val found = list.firstOrNull { it.chatId == chatId }
                    if (found != null) _uiState.update { it.copy(room = found) }
                }
            }
        }

        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { res ->
                res.fold(
                    onSuccess = { msgs ->
                        _uiState.update { it.copy(messages = msgs, isLoading = false, errorMessage = null) }
                        // 백엔드 규격: 화면이 열린 동안 정상 조회한 마지막 messageId로 읽음 처리
                        val lastId = msgs.lastOrNull()?.messageId
                        if (lastId != null && lastId != lastMarkedMessageId) {
                            lastMarkedMessageId = lastId
                            chatRepository.markAsRead(chatId, lastId)
                        }
                        // 채팅 이미지(gs://)를 getChatImage로 보안 로딩
                        msgs.filter { it.type == "IMAGE" }.forEach { msg ->
                            msg.imageUrls.indices.forEach { idx ->
                                loadChatImageIfNeeded(msg.messageId, idx)
                            }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = MarketErrorMapper.toMessage(e, "메시지를 불러오지 못했어요"))
                        }
                    }
                )
            }
        }
    }

    private fun loadChatImageIfNeeded(messageId: String, index: Int) {
        val key = "${messageId}_$index"
        if (key in _uiState.value.imageBytesCache || !loadingImageKeys.add(key)) return
        viewModelScope.launch {
            chatRepository.getChatImage(chatId, messageId, index).onSuccess { bytes ->
                _uiState.update { it.copy(imageBytesCache = it.imageBytesCache + (key to bytes)) }
            }
            loadingImageKeys.remove(key)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(messageInput = text, pendingTextRequestId = null, errorMessage = null) }
    }

    fun setPendingImages(uris: List<Uri>) {
        _uiState.update { it.copy(pendingImages = uris.take(10), pendingImageRequestId = null, errorMessage = null) }
    }

    fun removePendingImage(index: Int) {
        _uiState.update { s ->
            if (index !in s.pendingImages.indices) s
            else s.copy(pendingImages = s.pendingImages.toMutableList().apply { removeAt(index) }, pendingImageRequestId = null)
        }
    }

    fun sendMessage() {
        val s = _uiState.value
        val text = s.messageInput.trim()
        if (text.isEmpty() || s.isSending || s.isOpponentLeft) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            chatRepository.sendMessage(chatId, text, requestId = s.pendingTextRequestId).fold(
                onSuccess = {
                    _uiState.update { it.copy(messageInput = "", pendingTextRequestId = null, isSending = false) }
                },
                onFailure = { e ->
                    val opId = (e as? MarketException)?.operationId
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            pendingTextRequestId = opId ?: it.pendingTextRequestId,
                            errorMessage = MarketErrorMapper.toMessage(e, "전송 실패 · 다시 시도해 주세요.")
                        )
                    }
                }
            )
        }
    }

    fun sendPendingImages() {
        val s = _uiState.value
        if (s.pendingImages.isEmpty() || s.isSending || s.isOpponentLeft) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            chatRepository.sendImages(chatId, s.pendingImages, requestId = s.pendingImageRequestId).fold(
                onSuccess = {
                    _uiState.update { it.copy(pendingImages = emptyList(), pendingImageRequestId = null, isSending = false) }
                },
                onFailure = { e ->
                    val opId = (e as? MarketException)?.operationId
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            pendingImageRequestId = opId ?: it.pendingImageRequestId,
                            errorMessage = MarketErrorMapper.toMessage(e, "사진을 전송하지 못했어요.")
                        )
                    }
                }
            )
        }
    }

    fun leaveChatRoom(onLeft: () -> Unit) {
        viewModelScope.launch {
            chatRepository.leaveChatRoom(chatId).onSuccess {
                onLeft()
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = MarketErrorMapper.toMessage(e)) }
            }
        }
    }

    companion object {
        fun factory(
            chatId: String,
            authRepository: AuthRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatRoomViewModel(chatId, authRepository, chatRepository) as T
        }
    }
}
