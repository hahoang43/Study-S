package com.example.study_s.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_s.data.model.Group
import com.example.study_s.data.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll

// Giả sử GroupRepository có thể được cung cấp qua DI hoặc mặc định
class GroupViewModel(private val groupRepository: GroupRepository = GroupRepository()) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createSuccess = MutableStateFlow<String?>(null)
    val createSuccess: StateFlow<String?> = _createSuccess.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Thêm trạng thái lỗi để hiển thị phản hồi cho người dùng (nếu có lỗi tải nhóm)
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAllGroups()
    }

    /**
     * Tải tất cả các nhóm từ Repository và cập nhật trạng thái làm mới.
     * Logic này đã chính xác trong việc sử dụng khối try/finally.
     */
    fun loadAllGroups() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null // Xóa lỗi cũ
            try {
                _groups.value = groupRepository.getAllGroups()
            } catch (e: Exception) {
                // Xử lý và ghi nhận lỗi nếu có vấn đề khi tải
                _error.value = "Không thể tải nhóm: ${e.message}"
                println("Error loading groups: ${e.message}")
            } finally {
                // Đảm bảo trạng thái làm mới luôn tắt dù thành công hay thất bại
                _isRefreshing.value = false
            }
        }
    }

    fun loadUserGroups(userId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null // Xóa lỗi cũ
            try {
                _userGroups.value = groupRepository.getUserGroups(userId)
            } catch (e: Exception) {
                _error.value = "Không thể tải các nhóm đã tham gia: ${e.message}"
                println("Error loading user groups: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun getGroupById(groupId: String) {
        viewModelScope.launch {
            _group.value = groupRepository.getGroupById(groupId)
        }
    }

    fun createGroup(
        groupName: String,
        description: String,
        subject: String,
        creatorId: String
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            try {
                val newGroupId = FirebaseFirestore.getInstance().collection("groups").document().id
                val newGroup = Group(
                    groupId = newGroupId,
                    groupName = groupName,
                    description = description,
                    subject = subject,
                    members = listOf(creatorId),
                    createdBy = creatorId,
                    createdAt = System.currentTimeMillis()
                )
                groupRepository.createGroup(newGroup)
                _createSuccess.value = newGroupId
                
                val job1 = launch { loadAllGroups() }
                val job2 = launch { loadUserGroups(creatorId) }
                joinAll(job1, job2)

            } catch (e: Exception) {
                // Xử lý lỗi tạo nhóm
                _error.value = "Lỗi tạo nhóm: ${e.message}"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun joinGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.joinGroup(groupId, userId)
            val job1 = launch { loadAllGroups() }
            val job2 = launch { loadUserGroups(userId) }
            joinAll(job1, job2)
        }
    }

    fun leaveGroup(groupId: String, userId: String) {
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId, userId)
            val job1 = launch { loadAllGroups() }
            val job2 = launch { loadUserGroups(userId) }
            joinAll(job1, job2)
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId)
            loadAllGroups()
        }
    }
}