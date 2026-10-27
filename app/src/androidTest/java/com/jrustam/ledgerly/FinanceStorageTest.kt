package com.jrustam.ledgerly

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.repository.RoomFinanceRepository
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FinanceStorageTest {
    @Test fun walletsTransfersAndPlansSurviveReopenAndValidateDenominations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "finance-${java.util.UUID.randomUUID()}"
        var db = Room.databaseBuilder(context,TransactionDatabase::class.java,name).build()
        try {
            val repo=RoomFinanceRepository(db)
            val a=Wallet("a","Cash","","CASH","USD",10000,0,"")
            val b=a.copy(id="b",currency="UAH",openingMinor=0,lastFour="1234",kind="CARD")
            repo.saveWallet(a); repo.saveWallet(b)
            assertTrue(runCatching { repo.saveWallet(a.copy(currency="EUR")) }.isFailure)
            assertTrue(runCatching { repo.saveWallet(b.copy(lastFour="1234567890123456")) }.isFailure)
            val transfer=Transfer("transfer","a","b",1000,40000,LocalDate.now())
            repo.transfer(transfer); repo.transfer(transfer)
            assertTrue(runCatching { repo.transfer(transfer.copy(id="bad",toId="a")) }.isFailure)
            val goal=FinancePlan("goal",PlanKind.GOAL,"Trip","USD",50000,Category.FOOD,LocalDate.now())
            repo.savePlan(goal); repo.contribute(Contribution("c","goal",1000,LocalDate.now()))
            assertTrue(runCatching { repo.savePlan(goal.copy(currency="EUR")) }.isFailure)
            db.close()
            db=Room.databaseBuilder(context,TransactionDatabase::class.java,name).build()
            val reopened=RoomFinanceRepository(db)
            val state=reopened.observe().first()
            assertEquals(2,state.wallets.size)
            assertEquals(listOf(transfer),state.transfers)
            assertEquals(1000L,state.contributions.single().amountMinor)
            reopened.deleteTransfer(transfer.id)
            assertTrue(reopened.observe().first().transfers.isEmpty())
            reopened.deletePlan(goal.id)
            assertTrue(reopened.observe().first().contributions.isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
