package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.auth.SessionManager
import com.example.core.security.AppLockManager
import com.example.data.model.CustomerNumber
import com.example.data.model.UserType
import com.example.ui.components.AmanBottomNavigation
import com.example.ui.components.AmanTopAppBar
import com.example.ui.screens.admin.AdminAuditLogScreen
import com.example.ui.screens.admin.AdminCustomersScreen
import com.example.ui.screens.admin.AdminNotificationsScreen
import com.example.ui.screens.admin.AdminOverviewScreen
import com.example.ui.screens.admin.AdminPaymentTasksScreen
import com.example.ui.screens.admin.AdminPhonesScreen
import com.example.ui.screens.admin.AdminProtectionsScreen
import com.example.ui.screens.admin.AdminRequestsScreen
import com.example.ui.screens.admin.AdminSettingsScreen
import com.example.ui.screens.admin.AdminPackagesScreen
import com.example.ui.screens.admin.AdminTelecomScreen
import com.example.ui.screens.admin.AdminWalletsScreen
import com.example.ui.screens.auth.ForgotPasswordScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.PinLockScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.splash.AmanSplashScreen
import com.example.ui.screens.customer.AddPhoneDialog
import com.example.ui.screens.customer.AddedNumberSuccessDialog
import com.example.ui.screens.customer.CustomerAccountScreen
import com.example.ui.screens.customer.CustomerHomeScreen
import com.example.ui.screens.customer.CustomerNotificationsScreen
import com.example.ui.screens.customer.CustomerPhonesScreen
import com.example.ui.screens.customer.CustomerProtectionsScreen
import com.example.ui.screens.customer.CustomerRequestsScreen
import com.example.ui.screens.customer.CustomerSecurityScreen
import com.example.ui.screens.customer.CustomerHelpScreen
import com.example.ui.screens.customer.CustomerSettingsScreen
import com.example.ui.screens.customer.CustomerMoreScreen
import com.example.ui.screens.customer.ProtectionRequestDialog
import com.example.ui.screens.customer.AboutAmanScreen
import com.example.ui.screens.customer.TermsAndConditionsScreen
import com.example.ui.screens.admin.AdminTaskSettingsScreen
import com.example.ui.screens.admin.AdminEmployeesScreen
import com.example.ui.screens.admin.AdminRolesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AmanViewModel
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SessionManager.init(this)
        AppLockManager.init(this)
        setContent {
            MyApplicationTheme {
                // Enforce Arabic RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AmanApp()
                }
            }
        }
    }
}

@Composable
fun AmanApp(viewModel: AmanViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val currentRoute by viewModel.currentRoute.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val paymentWallets by viewModel.paymentWallets.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val systemSettings by viewModel.systemSettings.collectAsState()
    val telecomProviders by viewModel.telecomProviders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showAddPhoneDialog by remember { mutableStateOf(false) }
    var addedNumber by remember { mutableStateOf<CustomerNumber?>(null) }
    var selectedNumberForProtection by remember { mutableStateOf<CustomerNumber?>(null) }
    var authScreenRoute by remember { mutableStateOf("login") }
    var showWelcome by remember { mutableStateOf(true) }

    // Auth screens return early below, so the snackbar collector must be declared first.
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearUiMessage()
        }
    }

    if (isAppLocked) {
        PinLockScreen(viewModel = viewModel)
        return
    }

    if (isInitializing || showWelcome) {
        AmanSplashScreen(
            isReturningUser = isLoggedIn || currentUser != null,
            onFinished = { showWelcome = false }
        )
        return
    }

    if (!isLoggedIn || currentUser == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (authScreenRoute) {
                "login" -> LoginScreen(
                    viewModel = viewModel,
                    onNavigateToRegister = { authScreenRoute = "register" },
                    onNavigateToForgotPassword = { authScreenRoute = "forgot" }
                )
                "register" -> RegisterScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { authScreenRoute = "login" }
                )
                "forgot" -> ForgotPasswordScreen(
                    viewModel = viewModel,
                    onNavigateToLogin = { authScreenRoute = "login" }
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
            )
        }
        return
    }

    val user = currentUser ?: return

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AmanTopAppBar(
                unreadNotificationsCount = notifications.count { !it.isRead },
                onNotificationsClick = { viewModel.navigateTo("notifications") }
            )
        },
        bottomBar = {
            AmanBottomNavigation(
                currentUser = user,
                currentRoute = currentRoute,
                onNavigate = { viewModel.navigateTo(it) },
                onLogout = { viewModel.logout(); authScreenRoute = "login" }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentRoute,
                animationSpec = tween(durationMillis = 220),
                label = "aman_route_transition"
            ) { route ->
                if (user.userType == UserType.CUSTOMER) {
                    when (route) {
                        "home" -> CustomerHomeScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenAddPhone = { showAddPhoneDialog = true }
                        )
                        "phones" -> CustomerPhonesScreen(
                            viewModel = viewModel,
                            onProtectNumber = { selectedNumberForProtection = it },
                            onOpenAddPhone = { showAddPhoneDialog = true }
                        )
                        "requests" -> CustomerRequestsScreen(viewModel = viewModel)
                        "protections" -> CustomerProtectionsScreen(viewModel = viewModel, paymentMethods = paymentWallets, packages = packages)
                        "notifications" -> CustomerNotificationsScreen(viewModel = viewModel)
                        "account" -> CustomerAccountScreen(viewModel = viewModel)
                        "settings" -> CustomerSettingsScreen()
                        "security" -> CustomerSecurityScreen(viewModel = viewModel)
                        "help" -> CustomerHelpScreen()
                        "terms" -> TermsAndConditionsScreen()
                        "about" -> AboutAmanScreen()
                        else -> CustomerHomeScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenAddPhone = { showAddPhoneDialog = true }
                        )
                    }
                } else {
                    when (route) {
                        "overview" -> AdminOverviewScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                        "requests" -> AdminRequestsScreen(viewModel = viewModel)
                        "tasks" -> AdminPaymentTasksScreen(viewModel = viewModel)
                        "protections" -> AdminProtectionsScreen(viewModel = viewModel)
                        "customers" -> AdminCustomersScreen(viewModel = viewModel)
                        "phones" -> AdminPhonesScreen(viewModel = viewModel)
                        "telecom" -> AdminTelecomScreen(viewModel = viewModel)
                        "wallets" -> AdminWalletsScreen(viewModel = viewModel)
                        "audit" -> AdminAuditLogScreen(viewModel = viewModel)
                        "notifications" -> AdminNotificationsScreen(viewModel = viewModel)
                        "settings" -> AdminSettingsScreen(viewModel = viewModel)
                        "packages" -> AdminPackagesScreen(viewModel = viewModel, packagesOnly = true)
                        "task_settings" -> AdminTaskSettingsScreen(viewModel = viewModel)
                        "employees" -> AdminEmployeesScreen(viewModel = viewModel)
                        "roles" -> AdminRolesScreen(viewModel = viewModel)
                        "account" -> CustomerAccountScreen(viewModel = viewModel)
                        "security" -> CustomerSecurityScreen(viewModel = viewModel)
                        "help" -> CustomerHelpScreen()
                        "terms" -> TermsAndConditionsScreen()
                        "about" -> AboutAmanScreen()
                        else -> AdminOverviewScreen(
                            viewModel = viewModel,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                }
            }
            if (user.userType == UserType.CUSTOMER && currentRoute in setOf("home", "phones")) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = { showAddPhoneDialog = true },
                        containerColor = com.example.ui.theme.AmanTealDark,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(52.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Add,
                            contentDescription = "إضافة رقم",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Phone Dialog (Customer)
    if (showAddPhoneDialog) {
        AddPhoneDialog(
            onDismiss = { showAddPhoneDialog = false },
            providers = telecomProviders,
            isLoading = isLoading,
            onConfirm = { phone ->
                viewModel.addCustomerNumber(phone) { number ->
                    showAddPhoneDialog = false
                    addedNumber = number
                }
            }
        )
    }

    addedNumber?.let { number ->
        AddedNumberSuccessDialog(
            number = number,
            onDismiss = { addedNumber = null },
            onAddAnother = { addedNumber = null; showAddPhoneDialog = true },
            onActivateProtection = { addedNumber = null; selectedNumberForProtection = number }
        )
    }

    // Protection Request Dialog (Customer)
    selectedNumberForProtection?.let { num ->
        ProtectionRequestDialog(
            number = num,
            wallets = paymentWallets,
            packages = packages,
            settings = systemSettings,
            isLoading = isLoading,
            onDismiss = { selectedNumberForProtection = null },
            onSubmit = { packageId, walletId, ref ->
                viewModel.submitProtectionRequest(num.id, packageId, walletId, ref) {
                    selectedNumberForProtection = null
                    viewModel.navigateTo("requests")
                }
            }
        )
    }
}
