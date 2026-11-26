package com.caumarket.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.model.ChatRoom
import com.caumarket.app.core.model.ChatRoomInfo
import com.caumarket.app.data.repository.AuthRepository
import com.caumarket.app.data.repository.ChatRepository
import com.caumarket.app.ui.common.MarketErrorMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val currentUserId: String = "",
    val rooms: List<ChatRoom> = emptyList(),
    val roomInfos: Map<String, ChatRoomInfo> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val totalUnreadCount: Int
        get() = rooms.sumOf { it.unreadCount[currentUserId] ?: 0 }
}

class ChatListViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val uid = authRepository.getCurrentUser().getOrNull()?.userId.orEmpty()
            _uiState.update { it.copy(currentUserId = uid) }
            if (uid.isEmpty()) {
                _uiState.update { it.copy(rooms = emptyList(), isLoading = false) }
                return@launch
            }
            chatRepository.observeChatRooms().collect { result ->
                result.fold(
                    onSuccess = { rooms ->
                        _uiState.update { it.copy(rooms = rooms, isLoading = false, errorMessage = null) }
                        fetchMissingRoomInfos(rooms)
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = MarketErrorMapper.toMessage(e, "불러오지 못했어요"))
                        }
                    }
                )
            }
        }
    }

    fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
        _uiState.value = ChatListUiState()
    }

    private fun fetchMissingRoomInfos(rooms: List<ChatRoom>) {
        val cached = _uiState.value.roomInfos
        rooms.filter { it.chatId !in cached }.forEach { room ->
            viewModelScope.launch {
                chatRepository.getChatRoomInfo(room.chatId).onSuccess { info ->
                    _uiState.update { it.copy(roomInfos = it.roomInfos + (room.chatId to info)) }
                }
            }
        }
    }

    fun leaveChatRoom(chatId: String) {
        viewModelScope.launch {
            chatRepository.leaveChatRoom(chatId)
        }
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            chatRepository: ChatRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatListViewModel(authRepository, chatRepository) as T
        }
    }
}
