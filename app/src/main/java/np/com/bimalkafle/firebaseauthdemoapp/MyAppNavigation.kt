package np.com.bimalkafle.firebaseauthdemoapp

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import np.com.bimalkafle.firebaseauthdemoapp.network.SessionManager
import np.com.bimalkafle.firebaseauthdemoapp.pages.*
import np.com.bimalkafle.firebaseauthdemoapp.ui.chat.AiChatScreen
import np.com.bimalkafle.firebaseauthdemoapp.ui.chat.ChatListScreen
import np.com.bimalkafle.firebaseauthdemoapp.ui.chat.ChatScreen
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    splashViewModel: SplashViewModel,
    brandViewModel: BrandViewModel,
    influencerViewModel: InfluencerViewModel,
    campaignViewModel: CampaignViewModel,
    notificationViewModel: NotificationViewModel
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Any GraphQL/REST call that gets a 401/403 back from the backend fires this event
    // (see GraphQLClient/BackendRepository) so a stale/expired Firebase token doesn't
    // just leave the user stuck on a screen that silently keeps failing.
    LaunchedEffect(Unit) {
        SessionManager.sessionExpired.collectLatest {
            authViewModel.signout()
            Toast.makeText(context, "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash", modifier = modifier, builder = {
        composable("splash") {
            SplashScreen(navController, splashViewModel)
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable("login") {
            LoginPage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel
            )
        }
        composable("signup") {
            SignupPage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel
            )
        }
        composable("forgot_password") {
            ForgotPasswordPage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel
            )
        }
        composable("brand_registration") {
            BrandRegistrationScreen(
                onBack = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                onNext = { navController.navigate("brand_details") }
            )
        }
        composable("brand_details") {
            BrandDetailsScreen(
                onBack = { navController.popBackStack() },
                onCreateCampaign = { navController.navigate("create_campaign") },
                onGoToHome = { navController.navigate("brand_home") },
                brandViewModel = brandViewModel
            )
        }
        composable("create_campaign") {
            CreateCampaignScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("create_campaign_2") },
                campaignViewModel = campaignViewModel
            )
        }
        composable("create_campaign_2") {
            CreateCampaignScreen2(
                onBack = { navController.popBackStack() },
                onNext = { 
                    navController.navigate("campaign_details")
                },
                campaignViewModel = campaignViewModel
            )
        }
        composable("campaign_details") {
            CampaignDetailsPage(
                onBack = {
                    navController.navigate("brand_home") {
                        popUpTo("brand_home") { inclusive = true }
                    }
                },
                onSearchInfluencer = { navController.navigate("brand_home") },
                campaignViewModel = campaignViewModel
            )
        }
        composable("influencer_registration") {
            InfluencerRegistrationScreen(navController)
        }
        composable("influencer_detail") {
            InfluencerDetailScreen(
                onBack = { navController.popBackStack() },
                onApproachBrands = { navController.navigate("influencer_home") },
                influencerViewModel = influencerViewModel
          )
        }
        composable("influencer_create_proposal/{influencerId}") { backStackEntry ->
            val influencerId = backStackEntry.arguments?.getString("influencerId") ?: ""
            InfluencerCreateProposal(
                influencerId = influencerId,
                onBack = { navController.popBackStack() },
                onCreateProposal = { navController.navigate("brand_home") }, 
                brandViewModel = brandViewModel,
                authViewModel = authViewModel
            )
        }
        composable("brand_home") {
            BrandHomePage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel, 
                brandViewModel = brandViewModel,
                notificationViewModel = notificationViewModel
            )
        }
        composable("all_campaigns") {
            AllCampaignPage(navController, brandViewModel)
        }
        composable("brand_influencer_detail/{influencerId}") { backStackEntry ->
            val influencerId = backStackEntry.arguments?.getString("influencerId") ?: ""
            BrandInfluencerDetailScreen(
                influencerId = influencerId,
                onBack = { navController.popBackStack() },
                onCreateProposal = { id -> navController.navigate("influencer_create_proposal/$id") },
                onConnect = { id, name -> navController.navigate("chat/$id/$name") },
                influencerViewModel = influencerViewModel
            )
        }
        composable("brand_search") {
            BrandSearchPage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel, 
                brandViewModel = brandViewModel,
                notificationViewModel = notificationViewModel
            )
        }
        composable("brand_history") {
            ProposalPage (
                modifier = modifier,
                navController = navController,
                authViewModel = authViewModel,
                brandViewModel = brandViewModel,
                influencerViewModel = influencerViewModel
            )
        }
        composable("brand_profile") {
            BrandProfilePage(
                modifier = modifier, 
                navController = navController, 
                authViewModel = authViewModel, 
                brandViewModel = brandViewModel
            )
        }
        composable("brand_wishlist") {
            BrandWishlistPage(navController, brandViewModel)
        }

        composable("chatList") {
            ChatListScreen(
                onChatClick = { chatId, chatName, collaborationId ->
                    navController.navigate("chat/$chatId/$chatName?collaborationId=$collaborationId")
                },
                navController = navController
            )
        }

        composable(
            route = "chat/{chatId}/{chatName}?collaborationId={collaborationId}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType },
                navArgument("collaborationId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null 
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            val chatName = backStackEntry.arguments?.getString("chatName")
            val collaborationId = backStackEntry.arguments?.getString("collaborationId")
            ChatScreen(
                chatId = chatId,
                chatNameParam = chatName,
                collaborationId = collaborationId,
                navController = navController,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onCreateProposal = { id -> 
                    navController.navigate("influencer_create_proposal/$id")
                }
            )
        }

        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        composable("influencer_home") {
            InfluencerHomePage(
                modifier = modifier,
                navController = navController,
                authViewModel = authViewModel,
                influencerViewModel = influencerViewModel,
                campaignViewModel = campaignViewModel,
                notificationViewModel = notificationViewModel
            )
        }
        composable("influencer_search") {
            InfluencerSearchPage(
                modifier = modifier,
                navController = navController,
                campaignViewModel = campaignViewModel,
                notificationViewModel = notificationViewModel
            )
        }
        composable("proposals") {
            ProposalPage(
                modifier = Modifier,
                navController = navController,
                authViewModel = authViewModel,
                brandViewModel = brandViewModel,
                influencerViewModel = influencerViewModel
            )
        }
        composable("wishlist") {
            WishlistScreen(navController, campaignViewModel)
        }
        composable("influencerProfile") {
            InfluencerProfileScreen(
                modifier = modifier,
                navController = navController,
                authViewModel = authViewModel,
                influencerViewModel = influencerViewModel
            )
        }
        composable("discover") {
            DiscoverBrandsScreen(navController, notificationViewModel)
        }
        composable("campaign_analytics") {
            CampaignAnalyticsPage(navController)
        }
        composable(
            route = "campaign_detail/{campaignId}",
            arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            InfluencerBrandDetailScreen(
                navController = navController,
                campaignId = campaignId,
                campaignViewModel = campaignViewModel
            )
        }
        composable(
            route = "influencer_apply_campaign/{campaignId}",
            arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            InfluencerApplyCampaignScreen(
                campaignId = campaignId,
                onBack = { navController.popBackStack() },
                onApplySuccess = { 
                    navController.navigate("influencer_home") {
                        popUpTo("influencer_home") { inclusive = true }
                    }
                },
                campaignViewModel = campaignViewModel,
                influencerViewModel = influencerViewModel
            )
        }
        
        composable(
            route = "collaboration_analytics/{collaborationId}",
            arguments = listOf(navArgument("collaborationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val collaborationId = backStackEntry.arguments?.getString("collaborationId") ?: ""
            CollaborationAnalyticsPage(
                navController = navController,
                collaborationId = collaborationId,
                brandViewModel = brandViewModel,
                influencerViewModel = influencerViewModel
            )
        }
        composable("notifications") {
            NotificationPage(navController, notificationViewModel)
        }
        composable("ai_chat") {
            AiChatScreen(navController = navController)
        }
        composable(
            route = "brand_campaign_detail/{campaignId}",
            arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            BrandCampaignDetailScreen(
                campaignId = campaignId,
                navController = navController,
                campaignViewModel = campaignViewModel
            )
        }
    })
}
