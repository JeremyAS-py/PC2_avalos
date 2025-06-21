package com.example.pc2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pc2.auth.LoginScreen
import com.example.pc2.screens.MainScreen
import com.example.pc2.navigation.DrawerScaffold

@Composable
fun NavigationMenu() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("home") {
            DrawerScaffold(navController) {
                MainScreen()
            }
        }
    }
}