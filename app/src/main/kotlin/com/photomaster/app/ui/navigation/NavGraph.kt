package com.photomaster.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photomaster.app.ui.folder.FolderDetailScreen
import com.photomaster.app.ui.home.HomeScreen
import com.photomaster.app.ui.transfer.LanTransferScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object FolderDetail : Screen("folder/{folderId}?folderName={folderName}") {
        fun createRoute(folderId: String, folderName: String) =
            "folder/${android.net.Uri.encode(folderId)}?folderName=${android.net.Uri.encode(folderName)}"
    }
    data object Transfer : Screen("transfer")
}

@Composable
fun PhotoMasterNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onFolderClick = { folderId, folderName ->
                    navController.navigate(Screen.FolderDetail.createRoute(folderId, folderName))
                },
                onTransferClick = {
                    navController.navigate(Screen.Transfer.route)
                }
            )
        }
        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("folderName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStack ->
            val folderId = backStack.arguments?.getString("folderId") ?: return@composable
            val folderName = backStack.arguments?.getString("folderName") ?: ""
            FolderDetailScreen(
                folderId = folderId,
                folderName = folderName,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Transfer.route) {
            LanTransferScreen(onBack = { navController.popBackStack() })
        }
    }
}
