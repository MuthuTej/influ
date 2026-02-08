package np.com.bimalkafle.firebaseauthdemoapp

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.pages.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier, 
    authViewModel: AuthViewModel,
    splashViewModel: SplashViewModel,
    brandViewModel: BrandViewModel,
    influencerViewModel: InfluencerViewModel,
    campaignViewModel: CampaignViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash", modifier = modifier, builder = {
        composable("splash") {
            SplashScreen(navController, splashViewModel)
        }
        composable("onboarding") {
            OnboardingScreen(navController)
        }
        composable("login") {
            LoginPage(modifier, navController, authViewModel)
        }
        composable("signup") {
            SignupPage(modifier, navController, authViewModel)
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
                campaignViewModel = campaignViewModel,
                authViewModel = authViewModel
            )
        }
        composable("campaign_details") {
            CampaignDetailsPage(
                onBack = { navController.popBackStack() },
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
        composable("influencer_create_proposal") {
            InfluencerCreateProposal(
                onBack = { navController.popBackStack() },
                onCreateProposal = { navController.navigate("proposals") } // You can change this to navigate to the home screen
            )
        }
        composable("brand_home") {
            BrandHomePage(modifier, navController, authViewModel, brandViewModel)
        }
        composable("influencer_home") {
            InfluencerHomePage(modifier, navController, authViewModel)
        }
        composable("proposals") {
            ProposalPage(onBack = { navController.popBackStack() })
        }
        composable("wishlist") {
            WishlistScreen(navController)
        }
        composable("influencerProfile") {
            InfluencerProfileScreen(navController)
        }
        composable("discover") {
            DiscoverBrandsScreen(navController)
        }
    })
}
