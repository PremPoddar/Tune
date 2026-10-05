package com.prempoddar.tune.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.ui.detail.DetailScreen
import com.prempoddar.tune.ui.home.HomeScreen
import com.prempoddar.tune.ui.onboarding.OnboardingScreen
import com.prempoddar.tune.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun TuneNavHost(onboarded: Boolean) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    NavHost(
        navController = nav,
        startDestination = if (onboarded) "home" else "onboarding",
        enterTransition = { slideInHorizontally { it / 5 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 5 } + fadeOut() },
    ) {
        composable("onboarding") {
            OnboardingScreen(onDone = {
                scope.launch { AppGraph.settings.update { it.copy(onboarded = true) } }
                nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
            })
        }
        composable("home") {
            HomeScreen(
                onOpenApp = { nav.navigate("app/$it") },
                onOpenSettings = { nav.navigate("settings") },
                onFixPermissions = { nav.navigate("onboarding") },
            )
        }
        composable("app/{pkg}") { entry ->
            DetailScreen(pkg = entry.arguments?.getString("pkg").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
