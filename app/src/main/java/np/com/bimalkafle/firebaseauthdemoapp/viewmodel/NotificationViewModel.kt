package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.Notification
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import org.json.JSONObject

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _isLoading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _isLoading

    fun fetchNotifications(userId: String, token: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            val result = BackendRepository.getNotifications(token)
            result.onSuccess { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val data = jsonResponse.optJSONObject("data")
                    val notificationsArray = data?.optJSONArray("getNotifications")
                    val list = mutableListOf<Notification>()
                    if (notificationsArray != null) {
                        for (i in 0 until notificationsArray.length()) {
                            val obj = notificationsArray.getJSONObject(i)
                            list.add(parseNotification(obj))
                        }
                    }
                    _notifications.postValue(list)
                    _unreadCount.postValue(list.count { !it.isRead })
                    Log.d("NotificationViewModel", "Fetched ${list.size} notifications for user $userId")
                } catch (e: Exception) {
                    Log.e("NotificationViewModel", "Parsing error: ${e.message}", e)
                }
            }.onFailure {
                Log.e("NotificationViewModel", "Network error while fetching notifications", it)
            }
            _isLoading.postValue(false)
        }
    }

    fun fetchUnreadCount(userId: String, token: String) {
        viewModelScope.launch {
            val result = BackendRepository.getUnreadNotificationCount(token)
            result.onSuccess { count ->
                _unreadCount.postValue(count)
                Log.d("NotificationViewModel", "Unread count for user $userId: $count")
            }.onFailure {
                Log.e("NotificationViewModel", "Fetch unread count error for user $userId", it)
            }
        }
    }

    fun markAsRead(token: String, id: String) {
        viewModelScope.launch {
            val result = BackendRepository.markNotificationAsRead(token, id)
            result.onSuccess { success ->
                if (success) {
                    val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
                    val index = currentList.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val updated = currentList[index].copy(isRead = true)
                        currentList[index] = updated
                        _notifications.postValue(currentList)
                        _unreadCount.postValue(currentList.count { !it.isRead })
                        Log.d("NotificationViewModel", "Marked notification $id as read")
                    }
                }
            }.onFailure {
                Log.e("NotificationViewModel", "Mark as read error for notification $id", it)
            }
        }
    }

    fun markAllAsRead(token: String) {
        viewModelScope.launch {
            val result = BackendRepository.markAllNotificationsAsRead(token)
            result.onSuccess { success ->
                if (success) {
                    val currentList = _notifications.value?.map { it.copy(isRead = true) } ?: emptyList()
                    _notifications.postValue(currentList)
                    _unreadCount.postValue(0)
                    Log.d("NotificationViewModel", "Marked all notifications as read")
                }
            }.onFailure {
                Log.e("NotificationViewModel", "Mark all as read error", it)
            }
        }
    }

    private fun parseNotification(obj: JSONObject): Notification {
        return Notification(
            id = obj.getString("id"),
            userId = obj.getString("userId"),
            title = obj.getString("title"),
            body = obj.getString("body"),
            data = obj.optString("data", null),
            isRead = obj.optBoolean("isRead", false),
            createdAt = obj.optString("createdAt", "")
        )
    }
}
