package np.com.bimalkafle.firebaseauthdemoapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.pages.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel

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
            BrandRegistrationScreen(navController)
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