package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TransitEncryptedDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.TransitRepository
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PendingVipScreen
import com.example.ui.main.MainTransitScreen
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.TransitViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TransitEncryptedDatabase.getDatabase(applicationContext)

        setContent {
            MyApplicationTheme {
                val appScope = rememberCoroutineScope()

                val authRepository = remember {
                    AuthRepository(
                        vipCacheDao = database.vipCacheDao(),
                        appScope = appScope
                    )
                }

                val transitRepository = remember {
                        TransitRepository(
                            poiDao = database.poiDao(),
                            streetDao = database.streetDao(),
                            junctionDao = database.streetJunctionDao(),
                            database = database
                        )
                }

                val authViewModel = remember { AuthViewModel(authRepository) }
                val adminViewModel = remember { AdminViewModel(authRepository, transitRepository) }
                val transitViewModel = remember { TransitViewModel(transitRepository) }

                AppNavigationContainer(
                    authViewModel = authViewModel,
                    adminViewModel = adminViewModel,
                    transitViewModel = transitViewModel
                )
            }
        }
    }
}

@Composable
fun AppNavigationContainer(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    transitViewModel: TransitViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var isAdminDashboardMode by remember { mutableStateOf(false) }

    when (val state = authState) {
        is AuthState.Loading, is AuthState.Idle -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = IndigoSecondary,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        is AuthState.Unauthenticated, is AuthState.Error -> {
            isAdminDashboardMode = false
            AuthScreen(
                authViewModel = authViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        is AuthState.PendingVipApproval -> {
            isAdminDashboardMode = false
            PendingVipScreen(
                userProfile = state.profile,
                onSignOut = { authViewModel.signOut() },
                onSimulateAdminApproval = { authViewModel.simulateAdminApproval(true) },
                modifier = Modifier.fillMaxSize()
            )
        }

        is AuthState.Authenticated -> {
            if (state.profile.isAdmin && isAdminDashboardMode) {
                AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    currentUser = state.profile,
                    onBackToUserApp = { isAdminDashboardMode = false },
                    onSignOut = { authViewModel.signOut() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MainTransitScreen(
                    transitViewModel = transitViewModel,
                    userProfile = state.profile,
                    onOpenAdminPanel = { isAdminDashboardMode = true },
                    onSignOut = { authViewModel.signOut() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
