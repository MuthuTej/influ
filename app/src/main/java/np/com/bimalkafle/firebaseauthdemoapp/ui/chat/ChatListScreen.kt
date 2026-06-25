package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import np.com.bimalkafle.firebaseauthdemoapp.components.AppPullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatItem
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit,
    navController: NavController,
    viewModel: ChatViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState = authViewModel.authState.observeAsState()
    val chatList by viewModel.chatList.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var isBrand by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authState.value) {
        if (authState.value is np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated) {
            val role = (authState.value as np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated).role
            isBrand = role.equals("BRAND", ignoreCase = true)
            viewModel.loadChatList(role)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, "Couldn't load chats: $it", Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Connect Messenger",
                        color =MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            )
        },
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Connect",
                onItemSelected = { /* Handled in component */ },
                navController = navController,
                isBrand = isBrand
            )
        },
        floatingActionButton = {
            AiChatFab(navController)
        }
    ) { innerPadding ->
        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (authState.value is np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated) {
                    val role = (authState.value as np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated).role
                    isRefreshing = true
                    coroutineScope.launch {
                        viewModel.loadChatList(role)
                        isRefreshing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            if (chatList.isEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Default.ChatBubbleOutline,
                                title = "No conversations yet",
                                subtitle = "Start a collaboration to begin chatting."
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chatList) { entry ->
                        val chatItem = ChatItem(
                            chatId = entry.user.uid,
                            name = entry.user.name,
                            lastMessage = entry.lastMessage,
                            time = "", // You might want to format time here
                            unreadCount = entry.unreadCount,
                            profileImageUrl = entry.user.profileImageUrl
                        )
                        
                        ChatListItem(
                            chat = chatItem,
                            onClick = { onChatClick(entry.user.uid, entry.user.name) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }
}
