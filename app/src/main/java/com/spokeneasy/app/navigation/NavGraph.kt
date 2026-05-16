package com.spokeneasy.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spokeneasy.app.linking.LinkingListScreen
import com.spokeneasy.app.settings.SettingsScreen
import com.spokeneasy.app.word.WordListScreen

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object WordList : Screen("word_list", "单词学习", Icons.Default.MenuBook)
    data object LinkingList : Screen("linking_list", "连读练习", Icons.Default.RecordVoiceOver)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavScreens = listOf(Screen.WordList, Screen.LinkingList, Screen.Settings)

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.WordList.route
    ) {
        composable(Screen.WordList.route) { WordListScreen() }
        composable(Screen.LinkingList.route) { LinkingListScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
