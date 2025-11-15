// ĐƯỜNG DẪN: data/repository/PostRepository.kt
// NỘI DUNG HOÀN CHỈNH - PHIÊN BẢN CUỐI CÙNG

package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val postCollection = firestore.collection("posts")
    private val usersCollection = firestore.collection("users")

    /**
     * Tạo bài đăng mới
     */
    suspend fun createPost(post: PostModel) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val newPostRef = postCollection.document()

        val userDoc = usersCollection.document(userId).get().await()
        val currentUser = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

        val finalPost = post.copy(
            postId = newPostRef.id,
            authorId = userId,
            authorName = currentUser.name,
            authorAvatarUrl = currentUser.avatarUrl,
            contentLowercase = post.content.lowercase()
        )

        newPostRef.set(finalPost).await()
    }

    /**
     * Lấy toàn bộ danh sách bài đăng
     */
    suspend fun getAllPosts(): List<PostModel> {
        val snapshot = postCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }

    /**
     * Tìm kiếm bài viết (cho màn hình Search)
     */
    suspend fun searchPosts(query: String): List<PostModel> {
        if (query.isBlank()) {
            return emptyList()
        }
        return try {
            val searchQuery = query.lowercase()
            val endQuery = searchQuery + '\uf8ff'

            val querySnapshot = postCollection
                .whereGreaterThanOrEqualTo("contentLowercase", searchQuery)
                .whereLessThan("contentLowercase", endQuery)
                .limit(20)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(PostModel::class.java)?.apply { postId = doc.id }
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error searching posts", e)
            emptyList()
        }
    }

    /**
     * Lấy chi tiết 1 bài đăng theo ID
     */
    suspend fun getPostById(postId: String): PostModel? {
        val doc = postCollection.document(postId).get().await()
        return doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
    }

    /**
     * Xử lý Like/Unlike và trả về trạng thái 'isLiked' (true/false)
     */
    suspend fun toggleLike(postId: String, userId: String): Boolean {
        val postRef = postCollection.document(postId)
        return try {
            // ✅ SỬA "db" THÀNH "firestore"
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentLikes = snapshot.get("likedBy") as? List<String> ?: emptyList()

                if (currentLikes.contains(userId)) {
                    // --- ĐÃ LIKE -> BỎ LIKE ---
                    transaction.update(postRef, "likedBy", FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                    false // TRẢ VỀ false khi bỏ like
                } else {
                    // --- CHƯA LIKE -> THỰC HIỆN LIKE ---
                    transaction.update(postRef, "likedBy", FieldValue.arrayUnion(userId))
                    transaction.update(postRef, "likesCount", FieldValue.increment(1))
                    true // TRẢ VỀ true khi like
                }
            }.await()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error toggling like", e)
            false // Trả về false nếu có lỗi
        }
    }

    /**
     * Lấy danh sách bình luận cho 1 bài đăng
     */
    suspend fun getCommentsForPost(postId: String): List<CommentModel> {
        val snapshot = postCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommentModel::class.java)?.copy(commentId = doc.id)
        }
    }

    /**
     * Thêm bình luận mới
     */
    suspend fun addComment(postId: String, comment: CommentModel) {
        val postRef = postCollection.document(postId)
        val commentRef = postRef.collection("comments").document()

        val newComment = comment.copy(commentId = commentRef.id, postId = postId)

        firestore.batch()
            .set(commentRef, newComment)
            .update(postRef, "commentsCount", FieldValue.increment(1))
            .commit()
            .await()
    }

    /**
     * Lưu / Bỏ lưu bài viết
     */
    suspend fun toggleSavePost(postId: String, userId: String) {
        val postRef = postCollection.document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(PostModel::class.java)
                ?: throw Exception("Post not found")

            val savedBy = post.savedBy.toMutableList()
            val isSaved = savedBy.contains(userId)

            if (isSaved) {
                savedBy.remove(userId)
                transaction.update(postRef, "savedBy", savedBy)
            } else {
                savedBy.add(userId)
                transaction.update(postRef, "savedBy", savedBy)
            }
            null
        }.await()
    }

    /**
     * Lấy danh sách bài viết đã lưu của user
     */
    suspend fun getSavedPosts(userId: String): List<PostModel> {
        val snapshot = postCollection
            .whereArrayContains("savedBy", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }
}
