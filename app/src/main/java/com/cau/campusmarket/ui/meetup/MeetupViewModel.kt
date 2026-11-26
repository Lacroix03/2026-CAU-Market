package com.cau.campusmarket.ui.meetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.data.repository.ChatRepository
import com.cau.campusmarket.data.repository.MeetupRepository
import com.cau.campusmarket.domain.model.CampusPlace
import com.cau.campusmarket.domain.model.ChatRoom
import com.cau.campusmarket.domain.model.MeetupSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeetupUiState(
    val currentUserId: String = "",
    val room: ChatRoom? = null,
    val schedule: MeetupSchedule? = null,
    val selectedPlace: CampusPlace = CampusPlace.LIBRARY,
    val placeDetail: String = "",
    val dateText: String = "12월 10일 (목)",
    val timeText: String = "오후 3:00",
    val memo: String = "",
    val isSubmitting: Boolean = false,
    val feedbackMessage: String? = null
)

class MeetupViewModel(
    private val chatId: String,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val meetupRepository: MeetupRepository
) : ViewModel() {

    private val formFlow = MutableStateFlow(FormState())
    private val submittingFlow = MutableStateFlow(false)
    private val feedbackFlow = MutableStateFlow<String?>(null)

    private val roomFlow = chatRepository.observeChatRooms()
        .map { rooms -> rooms.firstOrNull { it.chatId == chatId } }

    private val coreFlow = combine(
        authRepository.currentUser,
        roomFlow,
        meetupRepository.observeMeetup(chatId)
    ) { user, room, schedule -> Triple(user?.userId.orEmpty(), room, schedule) }

    val uiState: StateFlow<MeetupUiState> = combine(
        coreFlow,
        formFlow,
        submittingFlow,
        feedbackFlow
    ) { core, form, submitting, feedback ->
        MeetupUiState(
            currentUserId = core.first,
            room = core.second,
            schedule = core.third,
            selectedPlace = form.place,
            placeDetail = form.placeDetail,
            dateText = form.dateText,
            timeText = form.timeText,
            memo = form.memo,
            isSubmitting = submitting,
            feedbackMessage = feedback
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeetupUiState())

    fun onPlaceSelect(p: CampusPlace) = formFlow.update { it.copy(place = p) }
    fun onPlaceDetailChange(v: String) = formFlow.update { it.copy(placeDetail = v) }
    fun onDateChange(v: String) = formFlow.update { it.copy(dateText = v) }
    fun onTimeChange(v: String) = formFlow.update { it.copy(timeText = v) }
    fun onMemoChange(v: String) = formFlow.update { it.copy(memo = v) }

    fun proposeSchedule() {
        val room = uiState.value.room ?: return
        val f = formFlow.value
        if (submittingFlow.value) return

        viewModelScope.launch {
            submittingFlow.value = true
            feedbackFlow.value = null
            meetupRepository.proposeMeetup(
                chatId = chatId,
                productId = room.productId,
                place = f.place,
                placeDetail = f.placeDetail,
                dateText = f.dateText,
                timeText = f.timeText,
                memo = f.memo
            ).fold(
                onSuccess = {
                    submittingFlow.value = false
                    feedbackFlow.value = "거래 일정을 제안했어요."
                },
                onFailure = { e ->
                    submittingFlow.value = false
                    feedbackFlow.value = e.message ?: "일정을 제안하지 못했어요."
                }
            )
        }
    }

    fun acceptSchedule() {
        if (submittingFlow.value) return
        viewModelScope.launch {
            submittingFlow.value = true
            meetupRepository.acceptMeetup(chatId).fold(
                onSuccess = {
                    submittingFlow.value = false
                    feedbackFlow.value = "거래 일정이 확정되었어요."
                },
                onFailure = { e ->
                    submittingFlow.value = false
                    feedbackFlow.value = e.message ?: "수락하지 못했어요."
                }
            )
        }
    }

    fun cancelSchedule() {
        if (submittingFlow.value) return
        viewModelScope.launch {
            submittingFlow.value = true
            meetupRepository.cancelMeetup(chatId).fold(
                onSuccess = {
                    submittingFlow.value = false
                    feedbackFlow.value = "거래 일정이 취소되었어요."
                },
                onFailure = { e ->
                    submittingFlow.value = false
                    feedbackFlow.value = e.message ?: "취소하지 못했어요."
                }
            )
        }
    }

    private data class FormState(
        val place: CampusPlace = CampusPlace.LIBRARY,
        val placeDetail: String = "",
        val dateText: String = "12월 10일 (목)",
        val timeText: String = "오후 3:00",
        val memo: String = ""
    )

    companion object {
        fun factory(
            chatId: String,
            authRepository: AuthRepository,
            chatRepository: ChatRepository,
            meetupRepository: MeetupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MeetupViewModel(chatId, authRepository, chatRepository, meetupRepository) as T
        }
    }
}
