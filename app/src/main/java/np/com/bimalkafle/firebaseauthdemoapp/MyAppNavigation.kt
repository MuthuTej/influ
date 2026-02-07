package np.com.bimalkafle.firebaseauthdemoapp

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.pages.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier, 
    authViewModel: AuthViewModel,
    splashViewModel: SplashViewModel
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
            BrandRegistrationScreen(onBack = { navController.popBackStack() }, onNext = { navController.navigate("brand_details") })
        }
        composable("brand_details") {
            BrandDetailsScreen(
                onBack = { navController.popBackStack() },
                onCreateCampaign = { navController.navigate("create_campaign") },
                onGoToHome = { navController.navigate("brand_home") }
            )
        }
        composable("create_campaign") {
            CreateCampaignScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("create_campaign_2") }
            )
        }
        composable("create_campaign_2") {
            CreateCampaignScreen2(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate("campaign_details") }
            )
        }
        composable("campaign_details") {
            CampaignDetailsPage(
                onBack = { navController.popBackStack() },
                onSearchInfluencer = { navController.navigate("brand_home") } // You can change this to navigate to the influencer search screen
            )
        }
        composable("influencer_registration") {
            InfluencerRegistrationScreen(navController)
        }
        composable("brand_home") {
            BrandHomePage(modifier, navController, authViewModel)
        }
        composable("influencer_home") {
            InfluencerHomePage(modifier, navController, authViewModel)
        }
    })
}