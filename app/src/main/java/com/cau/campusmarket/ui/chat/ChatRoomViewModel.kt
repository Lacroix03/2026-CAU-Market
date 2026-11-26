package com.cau.campusmarket.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.data.repository.MeetupRepository
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.MeetupSchedule
import com.cau.campusmarket.domain.model.Message
import com.cau.campusmarket.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatRoomUiState(
    val currentUserId: String = "",
    val room: ChatRoom? = null,
    val messages: List<Message> = emptyList(),
    val meetup: MeetupSchedule? = null,
    val messageInput: String = "",
    val pendingImages: List<String> = emptyList(),
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    val isOpponentLeft: Boolean
        get() = room != null && currentUserId.isNotEmpty() && room.isOpponentLeft(currentUserId)
}

class ChatRoomViewModel(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val meetupRepository: MeetupRepository
) : ViewModel() {

    private val inputFlow = MutableStateFlow("")
    private val pendingImagesFlow = MutableStateFlow<List<String>>(emptyList())
    private val sendingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    private val roomFlow = chatRepository.observeChatRooms()
        .map { rooms -> rooms.firstOrNull { it.chatId == chatId } }

    private val coreFlow = combine(
        authRepository.currentUser,
        roomFlow,
        chatRepository.observeMessages(chatId),
        meetupRepository.observeMeetup(chatId)
    ) { user, room, messages, meetup ->
        Quad(user?.userId.orEmpty(), room, messages, meetup)
    }

    val uiState: StateFlow<ChatRoomUiState> = combine(
        coreFlow,
        inputFlow,
        pendingImagesFlow,
        sendingFlow,
        errorFlow
    ) { core, input, pendingImages, sending, error ->
        ChatRoomUiState(
            currentUserId = core.a,
            room = core.b,
            messages = core.c,
            meetup = core.d,
            messageInput = input,
            pendingImages = pendingImages,
            isSending = sending,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatRoomUiState())

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.markAsRead(chatId)
     */
    fun markAsRead() {
        viewModelScope.launch {
            chatRepository.markAsRead(chatId)
        }
    }

    fun onInputChange(text: String) {
        inputFlow.value = text
        errorFlow.value = null
    }

    fun setPendingImages(uris: List<String>) {
        pendingImagesFlow.value = uris.take(Validators.MAX_CHAT_PHOTOS)
        errorFlow.value = null
    }

    fun removePendingImage(index: Int) {
        pendingImagesFlow.update { list ->
            if (index !in list.indices) list else list.toMutableList().apply { removeAt(index) }
        }
    }

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.sendMessage(chatId, text)
     */
    fun sendMessage() {
        val text = inputFlow.value.trim()
        if (text.isEmpty() || sendingFlow.value) return

        viewModelScope.launch {
            sendingFlow.value = true
            errorFlow.value = null
            chatRepository.sendMessage(chatId, text).fold(
                onSuccess = {
                    inputFlow.value = ""
                    sendingFlow.value = false
                },
                onFailure = { e ->
                    sendingFlow.value = false
                    errorFlow.value = e.message ?: "전송 실패 · 다시 시도해 주세요."
                }
            )
        }
    }

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.sendImages(chatId, imageUris)
     */
    fun sendPendingImages() {
        val images = pendingImagesFlow.value
        if (images.isEmpty() || sendingFlow.value) return

        viewModelScope.launch {
            sendingFlow.value = true
            errorFlow.value = null
            chatRepository.sendImages(chatId, images).fold(
                onSuccess = {
                    pendingImagesFlow.value = emptyList()
                    sendingFlow.value = false
                },
                onFailure = { e ->
                    sendingFlow.value = false
                    errorFlow.value = e.message ?: "사진을 전송하지 못했어요."
                }
            )
        }
    }

    /**
     * GitHub README 공통 함수 규격 호출:
     * chatRepository.leaveChatRoom(chatId)
     */
    fun leaveChatRoom(onLeft: () -> Unit) {
        viewModelScope.launch {
            chatRepository.leaveChatRoom(chatId)
            onLeft()
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        fun factory(
            chatId: String,
            authRepository: AuthRepository,
            chatRepository: ChatRepository,
            meetupRepository: MeetupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatRoomViewModel(chatId, authRepository, chatRepository, meetupRepository) as T
        }
    }
}
