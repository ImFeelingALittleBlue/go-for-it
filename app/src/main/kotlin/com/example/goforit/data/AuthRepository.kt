package com.example.goforit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.goforit.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// 登入狀態管理中心
// - 用 Credential Manager 叫出系統的 Google 帳號選擇器，拿到 Google 身分權杖
// - 再把權杖交給 Firebase Auth 換成正式登入
object AuthRepository {

    private val auth = Firebase.auth

    // user：目前登入的使用者。null 代表還沒登入。
    // 用 mutableStateOf 包起來，畫面 observe 它就能在登入/登出時自動切換
    var user by mutableStateOf<FirebaseUser?>(auth.currentUser)
        private set

    init {
        // Firebase 內部登入狀態一變動，就同步到上面的 user
        auth.addAuthStateListener { user = it.currentUser }
    }

    // 執行 Google 登入。成功回傳 Result.success，失敗回傳 Result.failure（附錯誤）
    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            // 1. 設定要向系統索取「Google 身分權杖」
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false) // false = 顯示全部 Google 帳號可選
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 2. 跳出系統帳號選擇器，使用者選好後拿到憑證
            val response = CredentialManager.create(context).getCredential(context, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)

            // 3. 把 Google 權杖交給 Firebase 換成正式登入
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            Result.success(Unit)
        } catch (e: GetCredentialException) {
            // 使用者取消、或沒設定好 Web client ID 時會走這裡
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 登出（清掉 Firebase 的登入狀態，回到登入頁）
    fun signOut() {
        auth.signOut()
    }
}

// 把 Firebase 的 Task 包成可以用 await() 的 suspend 函式
// （這樣登入流程才能一行一行依序等待，不用層層 callback）
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
