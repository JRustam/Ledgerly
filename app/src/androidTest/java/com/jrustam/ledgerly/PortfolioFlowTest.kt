package com.jrustam.ledgerly

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.jrustam.ledgerly.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Opt-in emulator portfolio capture. Fixtures never ship in production code. */
class PortfolioFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun captureAndVerifyCoreFlow() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("portfolioScreenshots") == "true")
        val context = instrumentation.targetContext
        val db = Room.databaseBuilder(context, TransactionDatabase::class.java, "ledgerly.db")
            .addMigrations(TransactionDatabase.MIGRATION_1_2).build()
        // Refuse to expose or replace an existing personal ledger in screenshots.
        assumeTrue(db.transactionDao().observeAll().first().isEmpty())
        val today = LocalDate.now().toEpochDay()
        val fixtures = listOf(
            TransactionEntity("portfolio-food", 8450, "USD", "EXPENSE", "FOOD", today, "Groceries for the week"),
            TransactionEntity("portfolio-salary", 420000, "USD", "INCOME", "SALARY", today - 1, "October salary"),
            TransactionEntity("portfolio-bills", 1499, "USD", "EXPENSE", "BILLS", today - 2, "Music subscription"),
            TransactionEntity("portfolio-freelance", 65000, "AZN", "INCOME", "FREELANCE", today - 3, "Design project"),
        )
        fixtures.forEach { db.transactionDao().insert(it) }
        var originalLocale = ""
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            originalLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        instrumentation.waitForIdleSync()
        scenario.recreate()
        fun capture(name: String) {
            closeSoftKeyboard()
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            val file = File(context.getExternalFilesDir("portfolio"), "$name.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Total balance").fetchSemanticsNodes().isNotEmpty() }
            capture("home")
            compose.onAllNodesWithText("Transactions").onLast().performClick()
            compose.onNodeWithText("Groceries for the week").assertExists()
            capture("history")
            compose.onNodeWithText("Groceries for the week").performClick()
            compose.onNodeWithText("Amount (USD)").performTextReplacement("89.25")
            capture("edit-transaction")
            compose.onNodeWithText("Save changes").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("89.25", substring = true).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(8925L, db.transactionDao().findById("portfolio-food")!!.amountMinor)
            compose.onAllNodesWithContentDescription("Transaction actions").onFirst().performClick()
            compose.onNodeWithText("Delete").performClick()
            compose.onNodeWithText("Delete transaction?").assertExists()
            compose.onNodeWithText("Cancel").performClick()
            assertNotNull(db.transactionDao().findById("portfolio-food"))
            compose.onNodeWithContentDescription("Add transaction").performClick()
            compose.onNodeWithText("Amount (USD)").performTextInput("125.00")
            compose.onNodeWithText("Income").performClick()
            compose.onNodeWithText("Freelance").performClick()
            capture("add-transaction")
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onAllNodesWithText("Settings").onLast().performClick()
            capture("settings")
            compose.onNodeWithText("English").performClick()
            compose.onNodeWithText("Azərbaycanca").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Tətbiqin dili").fetchSemanticsNodes().isNotEmpty() }
            capture("settings-az")
            scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("App language").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText("Home").onLast().performClick()
            scenario.onActivity { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Total balance").fetchSemanticsNodes().isNotEmpty() }
            capture("home-dark")
        } finally {
            scenario.close()
            fixtures.forEach { db.transactionDao().delete(it.id) }
            db.close()
            instrumentation.runOnMainSync {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(originalLocale))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
