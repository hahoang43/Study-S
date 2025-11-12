package com.example.study_s.data.repository
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val postCollection = firestore.collection("posts")

    // 🟢 Tạo bài đăng mới
    suspend fun createPost(post: PostModel) {
        val newPostRef = postCollection.document()
        // Sửa: postId được gán trong PostModel, không cần copy
        newPostRef.set(post).await()
    }

    // 🟢 Lấy toàn bộ danh sách bài đăng
    suspend fun getAllPosts(): List<PostModel> {
        val snapshot = postCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }

    // 🟢 Lấy chi tiết 1 bài đăng theo ID
    suspend fun getPostById(postId: String): PostModel? {
        val doc = postCollection.document(postId).get().await()
        return doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
    }

    // 🟢 MỚI: Thêm/Xóa Like (sử dụng Transaction)
    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = postCollection.document(postId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(PostModel::class.java)
                ?: throw Exception("Post not found")

            val likedBy = post.likedBy.toMutableList()
            val isLiked = likedBy.contains(userId)

            if (isLiked) {
                // User đã like -> Bỏ like
                likedBy.remove(userId)
                transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                transaction.update(postRef, "likedBy", likedBy)
            } else {
                // User chưa like -> Thêm like
                likedBy.add(userId)
                transaction.update(postRef, "likesCount", FieldValue.increment(1))
                transaction.update(postRef, "likedBy", likedBy)
            }
            null // Transaction success
        }.await()
    }

    // 🟢 MỚI: Lấy danh sách bình luận cho 1 bài đăng
    suspend fun getCommentsForPost(postId: String): List<CommentModel> {
        val snapshot = postCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommentModel::class.java)?.copy(commentId = doc.id)
        }
    }

    // 🟢 MỚI: Thêm bình luận mới
    suspend fun addComment(postId: String, comment: CommentModel) {
        val postRef = postCollection.document(postId)
        val commentRef = postRef.collection("comments").document() // Tạo ID mới

        val newComment = comment.copy(commentId = commentRef.id, postId = postId)

        // Sử dụng batched write để vừa thêm comment, vừa cập nhật count
        firestore.batch()
            .set(commentRef, newComment)
            .update(postRef, "commentsCount", FieldValue.increment(1))
            .commit()
            .await()
    }

    // ✅ HÀM MỚI: LƯU / BỎ LƯU BÀI VIẾT
    suspend fun toggleSavePost(postId: String, userId: String) {
        val postRef = postCollection.document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val post = snapshot.toObject(PostModel::class.java)
                ?: throw Exception("Post not found")

            val savedBy = post.savedBy.toMutableList()
            val isSaved = savedBy.contains(userId)

            if (isSaved) {
                // Đã lưu -> Bỏ lưu
                savedBy.remove(userId)
                transaction.update(postRef, "savedBy", savedBy)
            } else {
                // Chưa lưu -> Lưu
                savedBy.add(userId)
                transaction.update(postRef, "savedBy", savedBy)
            }
            null
        }.await()
    }

    // ✅ HÀM MỚI: LẤY DANH SÁCH BÀI VIẾT ĐÃ LƯU CỦA USER
    suspend fun getSavedPosts(userId: String): List<PostModel> {
        val snapshot = postCollection
            .whereArrayContains("savedBy", userId) // Tìm tất cả post có userId trong mảng 'savedBy'
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(PostModel::class.java)?.copy(postId = doc.id)
        }
    }
}