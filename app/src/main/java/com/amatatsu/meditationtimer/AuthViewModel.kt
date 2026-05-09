package com.amatatsu.meditationtimer

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// google-services.json から取得したウェブクライアントID
// res/values/strings.xml に定義する
private const val WEB_CLIENT_ID = "197034123500-r73f6om2u2rl7pkip24hs90d2lc7hd2j.apps.googleusercontent.com"

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // 起動時に既存のログイン状態を確認
        val current = auth.currentUser
        _authState.value = if (current != null) AuthState.Authenticated(current)
                           else AuthState.Unauthenticated
    }

    // メール+パスワードでサインアップ
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated(result.user!!)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "サインアップに失敗しました")
            }
        }
    }

    // メール+パスワードでログイン
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated(result.user!!)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "ログインに失敗しました")
            }
        }
    }

    // Googleでログイン
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                _authState.value = AuthState.Authenticated(authResult.user!!)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Googleログインに失敗しました")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
