package com.cau.campusmarket.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.domain.model.ChatRoom
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatListUiState(
    val currentUserId: String = "",
    val rooms: List<ChatRoom> = emptyList()
)

class ChatListViewModel(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.observeChatRooms()
     */
    val uiState: StateFlow<ChatListUiState> = combine(
        authRepository.currentUser,
        chatRepository.observeChatRooms()
    ) { user, rooms ->
        ChatListUiState(
            currentUserId = user?.userId.orEmpty(),
            rooms = rooms
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatListUiState())

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.leaveChatRoom(chatId)
     */
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
