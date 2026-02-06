package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

sealed class SplashState {
    object Loading : SplashState()
    object NavigateToOnboarding : SplashState()
    object NavigateToLogin : SplashState()
    data class NavigateToDashboard(val role: String) : SplashState()
    data class NavigateToRegistration(val role: String) : SplashState()
    data class Error(val message: String) : SplashState()
}

class SplashViewModel(private val prefsManager: PrefsManager) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _splashState = MutableLiveData<SplashState>()
    val splashState: LiveData<SplashState> = _splashState

    fun checkAppState() {
        _splashState.value = SplashState.Loading
        
        // 1. Check Onboarding
        if (!prefsManager.isOnboardingCompleted()) {
            _splashState.value = SplashState.NavigateToOnboarding
            return
        }

        // 2. Check Auth
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _splashState.value = SplashState.NavigateToLogin
            return
        }

        // 3. Login User -> Fetch Role & Check Profile Completion
        viewModelScope.launch {
            currentUser.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.token
                    if (token != null) {
                        viewModelScope.launch {
                            val result = BackendRepository.getUserRole(token)
                            result.onSuccess { role ->
                                // 4. Check Profile Completion
                                val isProfileCompleted = prefsManager.isProfileCompleted(currentUser.uid)
                                if (isProfileCompleted) {
                                    _splashState.value = SplashState.NavigateToDashboard(role)
                                } else {
                                    _splashState.value = SplashState.NavigateToRegistration(role)
                                }
                            }.onFailure {
                                _splashState.value = SplashState.Error("Failed to fetch role: ${it.message}")
                                // Fallback to login on error might be safer, but for now showing error
                                // _splashState.value = SplashState.NavigateToLogin 
                            }
                        }
                    } else {
                        _splashState.value = SplashState.NavigateToLogin
                    }
                } else {
                    _splashState.value = SplashState.NavigateToLogin
                }
            }
        }
    }
}

class SplashViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(PrefsManager(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
