package com.novelreader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novelreader.data.repository.BookRepository
import com.novelreader.ui.screens.BrowseMode
import com.novelreader.ui.screens.FileBrowserScreen
import com.novelreader.ui.screens.HomeScreen
import com.novelreader.ui.screens.ReaderScreen
import com.novelreader.ui.screens.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Reader : Screen("reader/{filePath}/{fileName}") {
        fun createRoute(filePath: String, fileName: String): String {
            val encodedPath = URLEncoder.encode(filePath, "UTF-8")
            val encodedName = URLEncoder.encode(fileName, "UTF-8")
            return "reader/$encodedPath/$encodedName"
        }
    }
    object Settings : Screen("settings")
    object FileBrowser : Screen("file_browser/{mode}") {
        fun createRoute(mode: BrowseMode): String {
            return "file_browser/${mode.name}"
        }
    }
}

@Composable
fun NavGraph(
    repository: BookRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onBookClick = { filePath, fileName ->
                    val route = Screen.Reader.createRoute(filePath, fileName)
                    navController.navigate(route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onBrowseFiles = {
                    navController.navigate(Screen.FileBrowser.createRoute(BrowseMode.FILES_AND_FOLDERS))
                },
                onBrowseFolders = {
                    navController.navigate(Screen.FileBrowser.createRoute(BrowseMode.FOLDERS_ONLY))
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val filePath = URLDecoder.decode(
                backStackEntry.arguments?.getString("filePath") ?: "",
                "UTF-8"
            )
            val fileName = URLDecoder.decode(
                backStackEntry.arguments?.getString("fileName") ?: "",
                "UTF-8"
            )
            ReaderScreen(
                filePath = filePath,
                fileName = fileName,
                repository = repository,
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FileBrowser.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val modeName = backStackEntry.arguments?.getString("mode") ?: "FILES_AND_FOLDERS"
            val mode = try {
                BrowseMode.valueOf(modeName)
            } catch (e: Exception) {
                BrowseMode.FILES_AND_FOLDERS
            }

            FileBrowserScreen(
                browseMode = mode,
                onFileSelected = { file ->
                    // Open the selected file
                    val route = Screen.Reader.createRoute(file.absolutePath, file.name)
                    navController.navigate(route) {
                        // Pop back to home after selecting file
                        popUpTo(Screen.FileBrowser.route) { inclusive = true }
                    }
                },
                onFolderSelected = { folder ->
                    // Import folder contents
                    // This would need to be handled by the HomeScreen
                    // For now, just navigate back
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
