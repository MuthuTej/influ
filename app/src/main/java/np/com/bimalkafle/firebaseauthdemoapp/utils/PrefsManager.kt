package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PROFILE_COMPLETED_PREFIX = "profile_completed_"
        private const val KEY_AI_CHAT_HISTORY_PREFIX = "ai_chat_history_"
    }

    fun saveOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun saveProfileCompleted(uid: String, completed: Boolean) {
        prefs.edit().putBoolean("$KEY_PROFILE_COMPLETED_PREFIX$uid", completed).apply()
    }

    fun isProfileCompleted(uid: String): Boolean {
        return prefs.getBoolean("$KEY_PROFILE_COMPLETED_PREFIX$uid", false)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // Raw JSON blob — AiChatViewModel owns the schema (it's the only reader/writer),
    // this is just per-uid scoped storage so switching accounts on one device
    // doesn't leak someone else's chat history.
    fun saveAiChatHistory(uid: String, json: String) {
        prefs.edit().putString("$KEY_AI_CHAT_HISTORY_PREFIX$uid", json).apply()
    }

    fun getAiChatHistory(uid: String): String? {
        return prefs.getString("$KEY_AI_CHAT_HISTORY_PREFIX$uid", null)
    }

    fun clearAiChatHistory(uid: String) {
        prefs.edit().remove("$KEY_AI_CHAT_HISTORY_PREFIX$uid").apply()
    }
}
