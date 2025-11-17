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

    private val commentCollection = firestore.collection("comments")


    /**
     * Tạo bài đăng mới
     */
    suspend fun createPost(post: PostModel) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val newPostRef = postCollection.document()
        val postWithId = post.copy(postId = newPostRef.id)
        val userDoc = usersCollection.document(userId).get().await()
        val currentUser = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

        val finalPost = post.copy(
            postId = newPostRef.id,
            authorId = userId,
            authorName = currentUser.name,
            authorAvatarUrl = currentUser.avatarUrl,
            contentLowercase = post.content.lowercase()
        )

        newPostRef.set(postWithId).await()
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
    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = postCollection.document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(PostModel::class.java)
                ?: throw Exception("Post not found")

            val likedBy = post.likedBy.toMutableList()
            if (likedBy.contains(userId)) {
                // Đã like -> Bỏ like
                likedBy.remove(userId)
                transaction.update(postRef, "likesCount", FieldValue.increment(-1))
            } else {
                // Chưa like -> Thêm like
                likedBy.add(userId)
                transaction.update(postRef, "likesCount", FieldValue.increment(1))
            }
            transaction.update(postRef, "likedBy", likedBy)
            null
        }.await()
    }

    /**
     * Lấy danh sách bình luận cho 1 bài đăng
     */
    suspend fun getCommentsForPost(postId: String): List<CommentModel> {
        val snapshot = commentCollection
            .whereEqualTo("postId", postId)
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
        val commentRef = commentCollection.document() // Tạo ID mới trong collection gốc

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
            if (savedBy.contains(userId)) {
                savedBy.remove(userId)
            } else {
                savedBy.add(userId)
            }
            transaction.update(postRef, "savedBy", savedBy)
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
    // ✍️ Sửa bài viết
    suspend fun updatePost(post: PostModel) {
        postCollection.document(post.postId).set(post).await()
    }

    // 🗑️ Xóa bài viết và dữ liệu liên quan
    suspend fun deletePost(postId: String) {
        val postRef = postCollection.document(postId)
        val commentsQuery = commentCollection.whereEqualTo("postId", postId).get().await()
        firestore.runTransaction { transaction ->
            // 1. Xóa tất cả các bình luận của bài viết từ collection gốc
            for (doc in commentsQuery.documents) {
                transaction.delete(doc.reference)
            }

            // 2. Cuối cùng, xóa chính bài viết đó
            transaction.delete(postRef)
        }.await()
    }
}
