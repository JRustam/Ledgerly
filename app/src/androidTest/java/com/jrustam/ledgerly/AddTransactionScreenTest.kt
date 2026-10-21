package com.jrustam.ledgerly

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import com.jrustam.ledgerly.domain.*
import com.jrustam.ledgerly.ui.addtransaction.*
import com.jrustam.ledgerly.ui.theme.LedgerlyTheme
import org.junit.Rule
import org.junit.Test

class AddTransactionScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun showsValidationThenSaveConfirmation() {
        compose.setContent {
            val viewModel = remember {
                AddTransactionViewModel(AddTransaction(object : FakeTransactionRepository() {
                    override suspend fun insert(transaction: Transaction) = Unit
                }), SavedStateHandle(), EditTransaction(FakeTransactionRepository()), FakeCurrencyPreferences())
            }
            val state by viewModel.state.collectAsState()
            LedgerlyTheme {
                AddTransactionScreen(state,
                    onAmount = { viewModel.edit(amount = it) },
                    onType = { viewModel.edit(type = it) },
                    onCategory = { viewModel.edit(category = it) },
                    onDate = { viewModel.edit(date = it) },
                    onDescription = { viewModel.edit(description = it) },
                    onSave = viewModel::save, onCurrency = { viewModel.edit(currency = it) })
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.save_transaction)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.category_error)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.amount_label, "USD")).performScrollTo().performTextInput("12.50")
        compose.onNodeWithText(compose.activity.getString(R.string.category_food)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.save_transaction)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.saved)).assertExists()
    }
}
