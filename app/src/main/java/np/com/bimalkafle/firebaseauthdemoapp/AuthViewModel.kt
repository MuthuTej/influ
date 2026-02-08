package np.com.bimalkafle.firebaseauthdemoapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
                                _authState.value = AuthState.Error(it.message ?: "Failed to fetch role")
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

    fun signup(email: String, password: String, name: String, role: String) {
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || role.isEmpty()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result?.token
                            if (token != null) {
                                viewModelScope.launch {
                                    val result = BackendRepository.signUp(name, role, token)
                                    result.onSuccess {
                                        // New signup: Profile is NOT completed yet
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
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
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
}