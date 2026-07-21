package np.com.bimalkafle.firebaseauthdemoapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository

class AuthViewModel : ViewModel() {

    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }


    fun checkAuthStatus() {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            fetchUserRole(user.uid)
        }
    }

    private fun fetchUserRole(uid: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            auth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.token
                    if (token != null) {
                        viewModelScope.launch {
                            val result = BackendRepository.getUserRole(token)
                            result.onSuccess { dataString ->
                                val parts = dataString.split("|")
                                val role = parts[0]
                                val isProfileCompletedBackend = parts.getOrNull(1)?.toBoolean() ?: false
                                _authState.value = AuthState.Authenticated(role, isProfileCompletedBackend)
                            }.onFailure {
                                // If user exists in Firebase but not in backend, treat as GoogleNewUser to finish signup
                                val msg = it.message ?: ""
                                if (msg.contains("not found", ignoreCase = true) || 
                                    msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                                    msg.contains("null at me", ignoreCase = true)) {
                                    _authState.value = AuthState.GoogleNewUser(auth.currentUser?.email ?: "", "")
                                } else {
                                    _authState.value = AuthState.Error(msg)
                                }
                            }
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Failed to get token")
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        fetchUserRole(user.uid)
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user exists in backend and get role
                        viewModelScope.launch {
                            user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result?.token
                                    if (token != null) {
                                        viewModelScope.launch {
                                            val result = BackendRepository.getUserRole(token)
                                            result.onSuccess { dataString ->
                                                val parts = dataString.split("|")
                                                val role = parts[0]
                                                val isProfileCompletedBackend = parts.getOrNull(1)?.toBoolean() ?: false
                                                _authState.value = AuthState.Authenticated(role, isProfileCompletedBackend)
                                            }.onFailure {
                                                // If role fetch fails, it might be a new user from Google
                                                val msg = it.message ?: ""
                                                if (msg.contains("not found", ignoreCase = true) || 
                                                    msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                                                    msg.contains("unauthorized", ignoreCase = true) ||
                                                    msg.contains("null at me", ignoreCase = true)) {
                                                    _authState.value = AuthState.GoogleNewUser(user.email ?: "", "")
                                                } else {
                                                    _authState.value = AuthState.Error(msg)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google Sign In failed")
                }
            }
    }

    fun signup(email: String, password: String, confirmPassword: String, name: String, role: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || role.isEmpty()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    completeBackendSignup(name, role)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun completeBackendSignup(name: String, role: String) {
        _authState.value = AuthState.Loading
        val user = auth.currentUser
        user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val token = tokenTask.result?.token
                if (token != null) {
                    viewModelScope.launch {
                        val result = BackendRepository.signUp(name, role, token)
                        result.onSuccess {
                            _authState.value = AuthState.Authenticated(role, false)
                        }.onFailure {
                            _authState.value = AuthState.Error(it.message ?: "Sync with backend failed")
                        }
                    }
                }
            } else {
                _authState.value = AuthState.Error("Failed to get token")
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isEmpty()) {
            _authState.value = AuthState.Error("Email can't be empty")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = BackendRepository.requestPasswordReset(email)
            result.onSuccess {
                _authState.value = AuthState.PasswordResetSent
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Failed to send reset link")
            }
        }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    companion object


}

sealed class AuthState {
    data class Authenticated(val role: String, val isProfileCompleted: Boolean) : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
    object PasswordResetSent : AuthState()
    data class GoogleNewUser(val email: String, val name: String) : AuthState()
}
