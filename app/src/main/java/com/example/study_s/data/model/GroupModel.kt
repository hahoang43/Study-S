package com.example.study_s.data.model

import com.google.firebase.firestore.Exclude

/**
 * Data class Group - phiên bản HOÀN CHỈNH dựa trên code của bạn.
 * - Sửa 'groupId' thành 'var' để có thể gán lại.
 * - Thêm 'groupNameLowercase' để tìm kiếm hiệu quả.
 * - Thêm constructor phụ cho Firebase.
 */
data class Group(
    val groupName: String = "",
    val description: String = "", // SỬ DỤNG 'description' LÀM CHUẨN
    val members: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long? = null,
    val subject: String = "",
    val avatarUrl: String? = null,
    val bannedUsers: List<String> = emptyList(),

    // Trường hỗ trợ tìm kiếm không phân biệt hoa thường
    val groupNameLowercase: String = groupName.lowercase(),
    val searchKeywords: List<String> = emptyList(),
    @get:Exclude
    var groupId: String = "" // PHẢI LÀ 'var'
) {
    // Constructor phụ mà Firebase cần
    constructor() : this(
        groupName = "",
        description = "",
        members = emptyList(),
        createdBy = "",
        createdAt = 0L,
        subject = "",
        avatarUrl = null,
        bannedUsers = emptyList(),
        groupNameLowercase = "",
        groupId = "",

    )
}
