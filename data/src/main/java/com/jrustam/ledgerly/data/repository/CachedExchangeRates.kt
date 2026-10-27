package com.jrustam.ledgerly.data.repository

import android.util.JsonReader
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.jrustam.ledgerly.domain.model.ExchangeQuote
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.repository.ExchangeRateProvider
import java.math.BigDecimal
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Network requests are user initiated; only currency codes are sent to Frankfurter. */
class CachedExchangeRates @Inject constructor(private val store: DataStore<Preferences>) :
    ExchangeRateProvider {
    private fun key(base: String, target: String) = stringPreferencesKey("fx_${base}_$target")
    override suspend fun quote(base: String, target: String): ExchangeQuote? {
        val raw = store.data.first()[key(base, target)] ?: return null
        return runCatching {
            val parts = raw.split('|'); ExchangeQuote(
            base,
            target,
            BigDecimal(parts[0]),
            parts[1].toLong(),
            parts[2]
        )
        }.getOrNull()
    }

    override suspend fun refresh(base: String, target: String): ExchangeQuote = withContext(Dispatchers.IO) {
        require(base != target && base in LedgerCurrency.entries.map { it.name } && target in LedgerCurrency.entries.map { it.name })
        val connection =
            URL("https://api.frankfurter.dev/v2/rate/$base/$target").openConnection() as HttpsURLConnection
        connection.connectTimeout = 10000; connection.readTimeout = 10000
        connection.instanceFollowRedirects = false
        try {
            check(connection.responseCode == 200)
            var rate: BigDecimal? = null;
            var date = "";
            var responseBase = "";
            var responseTarget = ""
            JsonReader(connection.inputStream.reader()).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) when (reader.nextName()) {
                    "rate" -> rate = BigDecimal(reader.nextString())
                    "date" -> date = reader.nextString()
                    "base" -> responseBase = reader.nextString()
                    "quote" -> responseTarget = reader.nextString()
                    else -> reader.skipValue()
                }
                reader.endObject()
            }
            require(responseBase == base && responseTarget == target && rate != null && rate!!.signum() > 0)
            java.time.LocalDate.parse(date)
            val result = ExchangeQuote(base, target, rate!!, System.currentTimeMillis(), date)
            store.edit {
                it[key(base, target)] =
                    "${result.rate.toPlainString()}|${result.fetchedAtMillis}|$date"
            }
            result
        } finally {
            connection.disconnect()
        }
    }
}
