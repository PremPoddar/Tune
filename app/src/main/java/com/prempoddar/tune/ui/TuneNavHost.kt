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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.R
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.ui.design.AboutScreen
import com.prempoddar.tune.ui.design.AppearanceScreen
import com.prempoddar.tune.ui.design.Credit
import com.prempoddar.tune.ui.design.LayeredIcon
import com.prempoddar.tune.ui.theme.TUNE_BRAND
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
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() }, onAppearance = { nav.navigate("appearance") }, onAbout = { nav.navigate("about") })
        }
        composable("appearance") {
            val settings by AppGraph.settings.settings.collectAsStateWithLifecycle(TuneSettings())
            AppearanceScreen(
                settings.appearance, TUNE_BRAND,
                onChange = { a -> scope.launch { AppGraph.settings.update { it.copy(appearance = a) } } },
                onBack = { nav.popBackStack() },
            )
        }
        composable("about") {
            AboutScreen(
                "Tune", "A volume for every app, remembered for every speaker and headphones.",
                icon = { LayeredIcon(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground) },
                credits = listOf(
                    Credit("Jetpack Compose and Glance", "Apache 2.0"),
                    Credit("Shizuku API", "Apache 2.0"),
                    Credit("HiddenApiBypass", "Apache 2.0"),
                    Credit("Material Color Utilities", "Apache 2.0"),
                    Credit("Nunito", "SIL Open Font License"),
                ),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
