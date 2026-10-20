package com.fileora.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fileora.app.ui.screens.batch.BatchProcessingScreen
import com.fileora.app.ui.screens.compressor.ImageCompressorScreen
import com.fileora.app.ui.screens.converter.ImageConverterScreen
import com.fileora.app.ui.screens.dashboard.DashboardScreen
import com.fileora.app.ui.screens.fileinspector.FileInspectorScreen
import com.fileora.app.ui.screens.history.HistoryScreen
import com.fileora.app.ui.screens.imagestopdf.ImagesToPdfScreen
import com.fileora.app.ui.screens.pdfinspector.PdfInspectorScreen
import com.fileora.app.ui.screens.pdfmerger.PdfMergerScreen
import com.fileora.app.ui.screens.resizer.ImageResizerScreen
import com.fileora.app.ui.screens.settings.SettingsScreen
import com.fileora.app.ui.screens.zipcreator.ZipCreatorScreen

@Composable
fun FileoraNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut(tween(300))
        }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRoute = { route -> navController.navigate(route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.ImageCompressor.route) {
            ImageCompressorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImageConverter.route) {
            ImageConverterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImageResizer.route) {
            ImageResizerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatchProcessing.route) {
            BatchProcessingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImagesToPdf.route) {
            ImagesToPdfScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfMerger.route) {
            PdfMergerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PdfInspector.route) {
            PdfInspectorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ZipCreator.route) {
            ZipCreatorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FileInspector.route) {
            FileInspectorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
