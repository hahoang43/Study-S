package com.example.study_s.data.repository

import android.util.Log
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.example.study_s.data.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val usersCollection = firestore.collection("users")
    private val postCollection = firestore.collection("posts")

    // BỎ private val commentCollection đi vì không dùng collection gốc nữa

    /**
     * Lấy danh sách bình luận cho 1 bài đăng TỪ SUB-COLLECTION.
     * ✅ ĐÃ SỬA
     */
    suspend fun getCommentsForPost(postId: String): List<CommentModel> {
        val snapshot = postCollection.document(postId) // 1. Đi vào bài viết
            .collection("comments")                 // 2. Lấy sub-collection "comments"
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommentModel::class.java)?.copy(commentId = doc.id)
        }
    }

    /**
     * Thêm bình luận mới VÀO SUB-COLLECTION.
     * ✅ ĐÃ SỬA
     */
    suspend fun addComment(postId: String, comment: CommentModel) {
        val postRef = postCollection.document(postId)

        // Tạo một document mới bên trong sub-collection "comments" của bài viết
        val commentRef = postRef.collection("comments").document()

        // Thêm các thông tin cần thiết và timestamp từ server
        val newCommentWithTimestamp = comment.copy(
            commentId = commentRef.id,
            postId = postId,
            timestamp = null // Timestamp sẽ được set bởi server
        )

        firestore.runBatch { batch ->
            // Thêm bình luận mới vào sub-collection
            batch.set(commentRef, newCommentWithTimestamp)
            // Cập nhật trường timestamp bằng giá trị của server
            batch.update(commentRef, "timestamp", FieldValue.serverTimestamp())
            // Tăng bộ đếm bình luận của bài viết
            batch.update(postRef, "commentsCount", FieldValue.increment(1))
        }.await()
    }


    /**
     * Cập nhật một bình luận trong SUB-COLLECTION.
     * ✅ ĐÚNG KIẾN TRÚC
     */
    suspend fun updateComment(postId: String, commentId: String, newContent: String) {
        val commentRef = postCollection.document(postId)
            .collection("comments")
            .document(commentId)

        commentRef.update("content", newContent).await()
    }

    /**
     * Xóa một bình luận khỏi SUB-COLLECTION.
     * ✅ ĐÚNG KIẾN TRÚC
     */
    suspend fun deleteComment(postId: String, commentId: String) {
        val postRef = postCollection.document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        firestore.runBatch { batch ->
            batch.delete(commentRef)
            batch.update(postRef, "commentsCount", FieldValue.increment(-1))
        }.await()
    }


    // ====================================================================
    // CÁC HÀM KHÁC (GIỮ NGUYÊN, KHÔNG CẦN THAY ĐỔI)
    // ====================================================================

    suspend fun createPost(post: PostModel) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val newPostRef = postCollection.document()
        val userDoc = usersCollection.document(userId).get().await()
        val currentUserModel = userDoc.toObject(UserModel::class.java) ?: throw Exception("User profile not found")

        val finalPost = post.copy(
            postId = newPostRef.id,
            authorId = userId,
            authorName = currentUserModel.name,
            authorAvatarUrl = currentUserModel.avatarUrl,
            contentLowercase = post.content.lowercase(),
            timestamp = null // Để server set
        )

        firestore.runBatch { batch ->
            batch.set(newPostRef, finalPost)
            batch.update(newPostRef, "timestamp", FieldValue.serverTimestamp())
        }.await()
    }

    suspend fun getAllPosts(): List<PostModel> {
        val snapshot = postCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .orderBy("__name__", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }

    suspend fun searchPosts(query: String): List<PostModel> {
        if (query.isBlank()) return emptyList()
        return try {
            val searchQuery = query.lowercase()
            val endQuery = searchQuery + '\uf8ff'
            val querySnapshot = postCollection
                .whereGreaterThanOrEqualTo("contentLowercase", searchQuery)
                .whereLessThan("contentLowercase", endQuery)
                .limit(20).get().await()
            querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(PostModel::class.java)?.apply { postId = doc.id }
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error searching posts", e)
            emptyList()
        }
    }

    suspend fun getPostById(postId: String): PostModel? {
        if (postId.isBlank()) {
            Log.e("PostRepository", "LỖI: getPostById được gọi với postId rỗng. Sẽ trả về null.")
            return null // Trả về null thay vì gây crash

        }
        val doc = postCollection.document(postId).get().await()
        return doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
    }

    suspend fun toggleLike(postId: String, userId: String): PostModel? {    val postRef = postCollection.document(postId)
        var updatedPost: PostModel? = null

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)

            // ✅ THAY ĐỔI QUAN TRỌNG NHẤT NẰM Ở ĐÂY
            // Thay vì dùng toObject() không, chúng ta sẽ dùng toObject() và copy() ngay lập tức
            // để đảm bảo postId từ document ID được đưa vào đối tượng.
            val post = snapshot.toObject(PostModel::class.java)?.copy(postId = snapshot.id)
                ?: throw Exception("Post not found")

            val likedBy = post.likedBy.toMutableList()
            val isLikedNow: Boolean

            if (likedBy.contains(userId)) {
                likedBy.remove(userId)
                isLikedNow = false
                transaction.update(postRef, "likesCount", FieldValue.increment(-1))
            } else {
                likedBy.add(userId)
                isLikedNow = true
                transaction.update(postRef, "likesCount", FieldValue.increment(1))
            }
            transaction.update(postRef, "likedBy", likedBy)

            // Cập nhật lại đối tượng post để trả về cho ViewModel
            // Vì "post" ở trên đã có postId đúng, nên updatedPost cũng sẽ có postId đúng.
            updatedPost = post.copy(
                likedBy = likedBy,
                likesCount = if(isLikedNow) post.likesCount + 1 else post.likesCount - 1
            )
        }.await()

        return updatedPost
    }

    suspend fun toggleSavePost(postId: String, userId: String) {
        val postRef = postCollection.document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(PostModel::class.java) ?: throw Exception("Post not found")
            val savedBy = post.savedBy.toMutableList()
            if (savedBy.contains(userId)) savedBy.remove(userId) else savedBy.add(userId)
            transaction.update(postRef, "savedBy", savedBy)
            null
        }.await()
    }

    suspend fun getSavedPosts(userId: String): List<PostModel> {
        val snapshot = postCollection
            .whereArrayContains("savedBy", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }

    suspend fun updatePost(post: PostModel) {
        postCollection.document(post.postId).set(post).await()
    }

    suspend fun deletePost(postId: String) {
        val postRef = postCollection.document(postId)
        val commentsQuery = postRef.collection("comments").get().await() // Query từ sub-collection
        firestore.runTransaction { transaction ->
            for (doc in commentsQuery.documents) {
                transaction.delete(doc.reference)
            }
            transaction.delete(postRef)
        }.await()
    }
}
