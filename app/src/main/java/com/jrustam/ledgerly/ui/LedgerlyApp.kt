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
import androidx.compose.runtime.saveable.rememberSaveable
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

import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.BalanceSummary
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.ui.finance.*
import com.jrustam.ledgerly.ui.common.*
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun LedgerlyRoot() {
    val product: ProductViewModel = hiltViewModel()
    val prefs by product.preferencesState.collectAsStateWithLifecycle()
    val failed by product.error.collectAsStateWithLifecycle()
    val dark = when(prefs?.appearance) { Appearance.DARK -> true; Appearance.LIGHT -> false; else -> isSystemInDarkTheme() }
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    com.jrustam.ledgerly.ui.theme.LedgerlyTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize()) {
            if (prefs == null) {
                if (failed) Text(stringResource(R.string.settings_save_error), Modifier.padding(24.dp))
                else LedgerLoadState(true, {})
            } else LedgerlyApp(product, prefs!!.onboarded, prefs!!.appearance)
        }
    }
}
@Composable
fun LedgerlyApp(product: ProductViewModel, onboarded: Boolean, appearance: Appearance) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val tabs = listOf("home", "transactions", "wallets", "plans", "insights")
    val labels = listOf(R.string.home, R.string.activity, R.string.wallets, R.string.plans, R.string.insights)
    val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.AccountBox, Icons.Default.DateRange, Icons.Default.Star)
    val settings: SettingsViewModel = hiltViewModel()
    val preferences by settings.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration) { settings.refreshLanguage() }
    val overview: OverviewViewModel = hiltViewModel()
    val ledger by overview.state.collectAsStateWithLifecycle()
    val finance: FinanceViewModel = hiltViewModel()
    val state by finance.state.collectAsStateWithLifecycle()
    val busy by finance.busy.collectAsStateWithLifecycle()
    val error by finance.error.collectAsStateWithLifecycle()
    val productError by product.error.collectAsStateWithLifecycle()
    fun navigate(destination: String) { nav.navigate(destination) { launchSingleTop = true } }
    fun back() { nav.popBackStack() }
    fun openTab(destination: String) { nav.navigate(destination) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }
    val motion = LocalMotionEnabled.current
    if (!onboarded) {
        FinancePage {
            LedgerMark()
            Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.welcome_body))
            com.jrustam.ledgerly.ui.settings.LanguageSelector(preferences.language, settings::selectLanguage)
            CurrencySelector(preferences.currency, !preferences.loading && !preferences.saving, onSelect = settings::selectCurrency)
            AppearanceChoice(appearance, product::appearance)
            if (productError || preferences.saveFailed || preferences.failed) Text(stringResource(R.string.settings_save_error), color = MaterialTheme.colorScheme.error)
            Button({ product.finish() }, enabled = !preferences.loading && !preferences.saving && !preferences.failed && !preferences.saveFailed) { Text(stringResource(R.string.get_started)) }
        }
        return
    }
    BackHandler(busy) {}
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (route != "add_transaction" && !route.startsWith("edit_transaction")) LedgerTopBar(
                stringResource(if (route in tabs) labels[tabs.indexOf(route)] else when {
                    route == "settings" -> R.string.settings
                    route == "profile" -> R.string.my_profile
                    route == "transfer" -> R.string.transfer
                    route == "connect" -> R.string.connect_bank
                    route == "currency_center" -> R.string.currency_center
                    route.startsWith("wallet") -> R.string.wallet
                    route.startsWith("plan") -> R.string.plans
                    else -> R.string.transaction_details
                }), onBack = if (route in tabs) null else ({ back() }), enabled = !busy,
                action = if (route in tabs) ({ IconButton({ navigate("settings") }) { Icon(Icons.Default.Settings,stringResource(R.string.settings)) } }) else null)
        },
        bottomBar = { if (route in tabs) LedgerNavigation(labels.map { stringResource(it) },icons,tabs.indexOf(route), { openTab(tabs[it]) }) },
        floatingActionButton = { if (route == "home" || route == "transactions") FloatingActionButton({ navigate("add_transaction") }) { Icon(Icons.Default.Add,stringResource(R.string.add_transaction)) } },
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (error) Row(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.save_error), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(finance::dismissError) { Text(stringResource(R.string.cancel)) }
            }
            NavHost(nav, startDestination = "home",
                enterTransition = { fadeIn(tween(if (motion) 180 else 0)) }, exitTransition = { fadeOut(tween(if (motion) 100 else 0)) }) {
                composable("home") {
                    if (state.loading || state.failed) LedgerLoadState(state.loading,finance::retry)
                    else HomeScreen(ledger,{ navigate("add_transaction") },{ openTab("transactions") },{ navigate("transaction/$it") },overview::retry,preferences.currency,
                        ledgerBalances = state.balances.currencies,
                        quickActions = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ openTab("wallets") }) { Text(stringResource(R.string.wallets)) }; OutlinedButton({ openTab("plans") }) { Text(stringResource(R.string.plans)) } }; MonthSummary(state.currentMonthTotals[preferences.currency.name] ?: BalanceSummary(),preferences.currency.name,java.time.YearMonth.now()) })
                }
                composable("transactions") { HistoryScreen(ledger,overview::setFilter,{ navigate("add_transaction") },{ navigate("transaction/$it") },overview::requestDelete,overview::retry) }
                composable("wallets") { FinanceContent(state,finance::retry) { WalletsScreen(state,::navigate) } }
                composable("plans") { FinanceContent(state,finance::retry) { PlansScreen(state,::navigate) } }
                composable("insights") { FinanceContent(state,finance::retry) { AnalyticsScreen(state,preferences.currency,::navigate,finance::selectMonth) } }
                composable("connect") { ConnectScreen() }
                composable("transfer") { FinanceContent(state,finance::retry) { TransferForm(state.data.wallets,busy) { finance.transfer(it,::back) } } }
                composable("wallet/{id}") { e -> FinanceContent(state,finance::retry) {
                    val id = e.arguments?.getString("id"); val wallet = state.data.wallets.find { it.id == id }
                    if (id == "new" || wallet != null) WalletForm(wallet,preferences.currency,busy) { finance.save(it,::back) } else MissingItem()
                } }
                composable("wallet_details/{id}") { e -> FinanceContent(state,finance::retry) {
                    state.data.wallets.find { it.id == e.arguments?.getString("id") }?.let { WalletDetails(it,state,::navigate) { id -> finance.deleteTransfer(id,{}) } } ?: MissingItem()
                } }
                composable("plan/{id}/{kind}") { e -> FinanceContent(state,finance::retry) {
                    val id = e.arguments?.getString("id"); val plan = state.data.plans.find { it.id == id }
                    val kind = runCatching { PlanKind.valueOf(e.arguments?.getString("kind").orEmpty()) }.getOrNull()
                    if (kind != null && (id == "new" || plan != null)) PlanForm(plan,kind,preferences.currency,busy) { finance.save(it,::back) } else MissingItem()
                } }
                composable("plan_details/{id}") { e -> FinanceContent(state,finance::retry) {
                    state.data.plans.find { it.id == e.arguments?.getString("id") }?.let { plan -> PlanDetails(plan,state,busy,::navigate,{ contribution, done -> finance.contribute(contribution,done) },{ finance.deletePlan(plan.id,::back) }) } ?: MissingItem()
                } }
                composable("transaction/{id}") { e ->
                    val row = ledger.transactions.find { it.id == e.arguments?.getString("id") }
                    if (row == null) MissingItem() else FinancePage {
                        Text(formatMoney(row.amountMinor,row.currency,appLocale()), style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(row.category.labelResource()), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(if (row.type == TransactionType.INCOME) R.string.income else R.string.expense))
                        Text(row.date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG).withLocale(appLocale())))
                        Text(state.data.wallets.find { it.id == row.walletId }?.name ?: stringResource(R.string.unassigned))
                        row.description?.let { Text(it) }
                        Button({ navigate("edit_transaction/${row.id}") }) { Text(stringResource(R.string.edit)) }
                        TextButton({ overview.requestDelete(row) }) { Text(stringResource(R.string.delete)) }
                    }
                }
                composable("settings") {
                    SettingsScreen(preferences, settings::selectLanguage, settings::selectCurrency, settings::retry,
                        appearance = appearance, onAppearance = product::appearance, productError = productError) {
                        TextButton({ navigate("profile") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Person, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.my_profile))
                        }
                        ExportAction(enabled = !state.loading && !state.failed)
                        TextButton({ navigate("currency_center") }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.currency_center))
                        }
                    }
                }
                composable("profile") { com.jrustam.ledgerly.ui.profile.ProfileScreen() }
                composable("currency_center") { CurrencyCenterScreen(product,preferences.currency) }
                composable("add_transaction") { TransactionEditor(::back,::back,state.data.wallets) }
                composable("edit_transaction/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.StringType })) { TransactionEditor(::back,::back,state.data.wallets) }
            }
        }
    }
    DeleteTransactionDialog(ledger,overview::confirmDelete,overview::cancelDelete)
}
@Composable private fun MissingItem() { Text(stringResource(R.string.not_found_title),Modifier.padding(20.dp)) }
@Composable private fun FinanceContent(state: FinanceState, retry: () -> Unit, content: @Composable () -> Unit) {
    if (state.loading || state.failed) LedgerLoadState(state.loading,retry) else content()
}
@Composable private fun AppearanceChoice(selected: Appearance, select: (Appearance) -> Unit) {
    Choice(stringResource(R.string.appearance), listOf(stringResource(R.string.appearance_system), stringResource(R.string.light), stringResource(R.string.dark)),selected.ordinal,{ select(Appearance.entries[it]) })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionEditor(onBack: () -> Unit, onSaved: () -> Unit, wallets: List<com.jrustam.ledgerly.domain.model.Wallet>) {
    val viewModel: AddTransactionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(state.saved) { if (state.saved) currentOnSaved() }

    BackHandler(enabled = state.isSaving) {

    }
    Column {
        LedgerTopBar(
            title = stringResource(if (state.isEditing) R.string.edit_transaction else R.string.add_transaction),
            onBack = onBack, enabled = !state.isSaving, includeStatusBar = false
        )
        when {
            state.isLoading || state.loadFailed -> LedgerLoadState(state.isLoading, viewModel::load)
            state.notFound -> Box(Modifier.padding(20.dp)) {
                EmptyPanel(
                    stringResource(R.string.not_found_title),
                    stringResource(R.string.not_found_body),
                    stringResource(R.string.back),
                    onBack
                )
            }

            else -> AddTransactionScreen(
                state,
                onAmount = { viewModel.edit(amount = it) },
                onType = { viewModel.edit(type = it) },
                onCategory = { viewModel.edit(category = it) },
                onDate = { viewModel.edit(date = it) },
                onDescription = { viewModel.edit(description = it) },
                onSave = viewModel::save,
                onCurrency = { viewModel.edit(currency = it) },
                wallets = wallets, onWallet = viewModel::selectWallet, showHeading = false
            )
        }
    }
}
