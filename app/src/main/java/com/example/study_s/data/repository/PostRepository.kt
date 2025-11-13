package com.example.study_s.data.repository
import android.util.Log
import com.example.study_s.data.model.CommentModel
import com.example.study_s.data.model.PostModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.study_s.data.model.User // <-- Import model User
import com.google.firebase.auth.FirebaseAuth
class PostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
            private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val postCollection = firestore.collection("posts")
    private val usersCollection = firestore.collection("users")
    // 🟢 Tạo bài đăng mới
    // PHIÊN BẢN ĐÃ SỬA (ĐÚNG)
    suspend fun createPost(post: PostModel) {
        // 1. Lấy ID của người dùng đang đăng nhập.
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val newPostRef = postCollection.document()

        // 2. Dùng ID đó để lấy toàn bộ thông tin profile của người dùng từ collection 'users'.
        val userDoc = usersCollection.document(userId).get().await()
        val currentUser = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

        // 3. TẠO RA một đối tượng `finalPost` HOÀN CHỈNH.
        // Nó lấy thông tin gốc từ 'post' (content, imageUrl) và bổ sung thêm các thông tin còn thiếu.
        val finalPost = post.copy(
            postId = newPostRef.id,
            authorId = userId,
            authorName = currentUser.name,         // <-- Lấy từ profile
            authorAvatarUrl = currentUser.avatarUrl, // <-- Lấy từ profile
            contentLowercase = post.content.lowercase() // <-- Tự tính toán
        )

        // 4. Lưu đối tượng HOÀN CHỈNH này lên Firestore.
        newPostRef.set(finalPost).await()
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
    /**
     * HÀM TÌM KIẾM BÀI VIẾT (CHO MÀN HÌNH SEARCH)
     * Tìm kiếm không phân biệt hoa thường trên trường 'contentLowercase' của bài viết.
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