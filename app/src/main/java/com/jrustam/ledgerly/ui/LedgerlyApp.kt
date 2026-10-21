package com.jrustam.ledgerly.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jrustam.ledgerly.ui.design.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalConfiguration
import com.jrustam.ledgerly.ui.settings.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.jrustam.ledgerly.R
import com.jrustam.ledgerly.ui.addtransaction.*
import com.jrustam.ledgerly.ui.overview.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerlyApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val tabs = listOf("home", "transactions", "insights", "settings")
    val labels = listOf(R.string.home, R.string.transactions, R.string.insights, R.string.settings)
    val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.Star, Icons.Default.Settings)
    val settings: SettingsViewModel = hiltViewModel()
    val preferences by settings.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) { settings.refreshLanguage() }
    val overview: OverviewViewModel = hiltViewModel()
    val state by overview.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(route) { if (route !in tabs) snackbar.currentSnackbarData?.dismiss() }
    val savedMessage = stringResource(R.string.saved)
    fun openTab(destination: String) {
        navController.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val motion = LocalMotionEnabled.current
    Scaffold(
        modifier = Modifier.background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)))),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (route in tabs) LedgerTopBar(
                title = stringResource(if (route == "home") R.string.app_name else labels[tabs.indexOf(route)]),
                action = if (route == "home") ({
                    IconButton(onClick = { openTab("settings") }) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), modifier = Modifier.size(20.dp))
                    }
                }) else null,
            )
        },
        bottomBar = {
            if (route in tabs) LedgerNavigation(labels.map { stringResource(it) }, icons, tabs.indexOf(route)) { openTab(tabs[it]) }
        },
        floatingActionButton = {
            if (route == "home" || route == "transactions") {
                FloatingActionButton(onClick = { navController.navigate("add_transaction") },
                    shape = LedgerShapes.control, containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_transaction))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = "home",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(if (motion) 220 else 0)) + slideInHorizontally(tween(if (motion) 220 else 0)) { it / 20 } },
            exitTransition = { fadeOut(tween(if (motion) 140 else 0)) },
            popEnterTransition = { fadeIn(tween(if (motion) 220 else 0)) + slideInHorizontally(tween(if (motion) 220 else 0)) { -it / 20 } },
            popExitTransition = { fadeOut(tween(if (motion) 140 else 0)) },
        ) {
            composable("home") {
                HomeScreen(state, onAdd = { navController.navigate("add_transaction") },
                    onHistory = { openTab("transactions") },
                    onEdit = { navController.navigate("edit_transaction/$it") }, onRetry = overview::retry, defaultCurrency = preferences.currency)
            }
            composable("transactions") {
                HistoryScreen(state, overview::setFilter, onAdd = { navController.navigate("add_transaction") },
                    onEdit = { navController.navigate("edit_transaction/$it") },
                    onDelete = overview::requestDelete, onRetry = overview::retry)
            }
            composable("insights") {
                PlaceholderScreen(R.string.insights, R.string.insights_body)
            }
            composable("settings") {
                SettingsScreen(preferences, settings::selectLanguage, settings::selectCurrency, settings::retry)
            }
            composable("add_transaction") {
                TransactionEditor(onBack = { navController.popBackStack() }, onSaved = {
                    navController.popBackStack()
                    scope.launch { snackbar.showSnackbar(savedMessage) }
                })
            }
            composable("edit_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType })) {
                TransactionEditor(onBack = { navController.popBackStack() }, onSaved = {
                    navController.popBackStack()
                    scope.launch { snackbar.showSnackbar(savedMessage) }
                })
            }
        }
    }
    DeleteTransactionDialog(state, overview::confirmDelete, overview::cancelDelete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditor(onBack: () -> Unit, onSaved: () -> Unit) {
    val viewModel: AddTransactionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(state.saved) { if (state.saved) currentOnSaved() }
    BackHandler(enabled = state.isSaving) { /* Finish the write before leaving. */ }
    Column {
        LedgerTopBar(title = stringResource(if (state.isEditing) R.string.edit_transaction else R.string.add_transaction),
            onBack = onBack, enabled = !state.isSaving, includeStatusBar = false)
        when {
            state.isLoading || state.loadFailed -> LedgerLoadState(state.isLoading, viewModel::load)
            state.notFound -> Box(Modifier.padding(20.dp)) {
                EmptyPanel(stringResource(R.string.not_found_title), stringResource(R.string.not_found_body),
                    stringResource(R.string.back), onBack)
            }
            else -> AddTransactionScreen(state,
                onAmount = { viewModel.edit(amount = it) }, onType = { viewModel.edit(type = it) },
                onCategory = { viewModel.edit(category = it) }, onDate = { viewModel.edit(date = it) },
                onDescription = { viewModel.edit(description = it) }, onSave = viewModel::save,
                onCurrency = { viewModel.edit(currency = it) }, showHeading = false)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: Int, body: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        EmptyPanel(stringResource(R.string.coming_later), stringResource(body))
    }
}
