package com.example.study_s.data.repository
import com.example.study_s.data.model.PostModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val postCollection = firestore.collection("posts")

    // 🟢 Tạo bài đăng mới
    suspend fun createPost(post: PostModel) {
        val newPostRef = postCollection.document()
        val newPost = post.copy(postId = newPostRef.id)
        newPostRef.set(newPost).await()
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
}
