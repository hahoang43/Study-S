package com.example.study_s.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Group
import com.example.study_s.data.model.Message
import com.example.study_s.data.repository.GroupChatRepository
import com.example.study_s.data.repository.GroupRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val chatRepository: GroupChatRepository = GroupChatRepository()
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            _groups.value = groupRepository.getUserGroups(userId)
        }
    }

    fun listenToGroupMessages(groupId: String) {
        listenerRegistration?.remove()
        listenerRegistration = chatRepository.listenForMessages(groupId) { newMessages ->
            _messages.value = newMessages
        }
    }

    fun sendMessage(groupId: String, senderId: String, content: String) {
        viewModelScope.launch {
            val message = Message(
                senderId = senderId,
                content = content
            )
            chatRepository.sendMessage(groupId, message)
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}
