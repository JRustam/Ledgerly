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
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.entity.TransactionEntity
import com.jrustam.ledgerly.data.local.entity.WalletEntity
import com.jrustam.ledgerly.data.local.entity.PlanEntity
import com.jrustam.ledgerly.data.local.entity.ContributionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** Opt-in dedicated emulator workflow. No fixtures ship in production. */
class PortfolioFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun captureAndVerifyCoreFlow() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("portfolioScreenshots")=="true")
        val context=instrumentation.targetContext
        val db=Room.databaseBuilder(context,TransactionDatabase::class.java,"ledgerly.db")
            .addMigrations(TransactionDatabase.MIGRATION_1_2,TransactionDatabase.MIGRATION_2_3).build()
        assumeTrue(db.transactionDao().observeAll().first().isEmpty() && db.financeDao().wallets().first().isEmpty())
        val today=LocalDate.now().toEpochDay()
        val rows=listOf(
            TransactionEntity("portfolio-food",8450,"USD","EXPENSE","FOOD",today,"Groceries for the week","portfolio-wallet"),
            TransactionEntity("portfolio-salary",420000,"USD","INCOME","SALARY",today-1,"October salary","portfolio-wallet"),
            TransactionEntity("portfolio-bills",1499,"USD","EXPENSE","BILLS",today-2,"Music subscription","portfolio-wallet"),
            TransactionEntity("portfolio-freelance",65000,"AZN","INCOME","FREELANCE",today-3,"Design project"))
        rows.forEach { db.transactionDao().insert(it) }
        db.financeDao().save(WalletEntity("portfolio-wallet","Everyday","Manual account","CARD","USD",0,0,"2048"))
        db.financeDao().save(WalletEntity("portfolio-cash","Travel cash","","CASH","EUR",120000,1,""))
        db.financeDao().save(PlanEntity("portfolio-budget","BUDGET","Food this month","USD",40000,"FOOD",today))
        db.financeDao().save(PlanEntity("portfolio-goal","GOAL","Summer in Lisbon","EUR",250000,"FOOD",today+180))
        db.financeDao().insert(ContributionEntity("portfolio-contribution","portfolio-goal",75000,today-14))
        db.financeDao().save(PlanEntity("portfolio-recurring","RECURRING","Music subscription","USD",1499,"BILLS",today+3))
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        var original=""
        scenario.onActivity { original=AppCompatDelegate.getApplicationLocales().toLanguageTags();AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
        instrumentation.waitForIdleSync(); scenario.recreate()
        fun capture(name: String) {
            closeSoftKeyboard(); compose.mainClock.advanceTimeBy(500); compose.waitForIdle()
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(context.getExternalFilesDir("portfolio"),"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            }
        }
        fun text(value: String)=compose.onAllNodesWithText(value).onLast()
        try {
            compose.waitUntil(10000) { compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Total balance").fetchSemanticsNodes().isNotEmpty() }
            if (compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()) text("Get started").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Total balance").fetchSemanticsNodes().isNotEmpty() }
            capture("home")
            text("Activity").performClick();text("Groceries for the week").assertExists();capture("history")
            text("Groceries for the week").performClick();text("Edit").performClick()
            compose.onNodeWithText("Amount (USD)").performTextReplacement("89.25");capture("edit-transaction")
            text("Save changes").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("89.25",substring=true).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(8925L,db.transactionDao().findById("portfolio-food")!!.amountMinor)
            text("Delete").performClick();text("Delete transaction?").assertExists();text("Cancel").performClick()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithContentDescription("Add transaction").performClick()
            compose.onNodeWithText("Amount (USD)").performTextInput("125.00")
            text("Income").performScrollTo().performClick();text("Freelance").performScrollTo().performClick();capture("add-transaction")
            compose.onNodeWithContentDescription("Back").performClick()
            text("Wallets").performClick();capture("wallets");text("Everyday").performClick();capture("wallet-details")
            compose.onNodeWithContentDescription("Back").performClick();text("Transfer").performClick()
            text("Amount sent").performTextInput("10.00");text("Amount received").performTextInput("9.00")
            text("Transfer").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("A home for every balance.").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1,db.financeDao().transfers().first().size)
            text("Plans").performClick();capture("plans");text("Food this month").performClick();capture("budget-details")
            compose.onNodeWithContentDescription("Back").performClick()
            text("Insights").performClick();capture("insights")
            compose.onNodeWithContentDescription("Settings").performClick();capture("settings")
            text("Appearance: System language").performClick();text("Dark").performClick()
            compose.onNodeWithContentDescription("Back").performClick();text("Home").performClick();capture("home-dark")
            compose.onNodeWithContentDescription("Settings").performClick()
            text("Appearance: Dark").performClick();text("System language").performClick()
        } finally {
            scenario.close()
            db.openHelper.writableDatabase.apply {
                execSQL("DELETE FROM transfers WHERE fromId LIKE 'portfolio-%' OR toId LIKE 'portfolio-%'")
                execSQL("DELETE FROM contributions WHERE id LIKE 'portfolio-%'")
                execSQL("DELETE FROM plans WHERE id LIKE 'portfolio-%'")
                execSQL("DELETE FROM wallets WHERE id LIKE 'portfolio-%'")
            }
            rows.forEach { db.transactionDao().delete(it.id) };db.close()
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(original)) }
        }
    }
}
