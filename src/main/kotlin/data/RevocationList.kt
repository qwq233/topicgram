@file:Suppress("MemberVisibilityCanBePrivate")

package top.qwq2333.data

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.SerialName as Name

val cache: Cache<String, String> = CacheBuilder.newBuilder()
    .maximumSize(128)
    .build()

@Serializable
data class RevocationList(
    @Name("entries") val entries: Map<String, Entry>,
)

object DbRevocationList {
    init {
        transaction(database) {
            SchemaUtils.create(ListTable)
        }
        doUpdateList()
    }

    fun doUpdateList() = CoroutineScope(Dispatchers.IO).launch {
        client.get("https://android.googleapis.com/attestation/status?" + System.currentTimeMillis()).let { response ->
            val list: RevocationList = Json.decodeFromString(response.body())
            var count = count()
            if (list.entries.size.toLong() <= count) return@launch

            // assume that the sn of the newly revoked keybox must be at the bottom of the revocation list
            list.entries.toList().asReversed().forEach {
                if (query(it.first) != null && list.entries.size.toLong() <= count) {
                    return@launch
                }
                create(it.first, it.second)
                count++
            }
        }
    }

    object ListTable : Table("revocation_list") {
        val id = integer(name = "id").autoIncrement()
        val sn = varchar("sn", length = 50)

        val status = varchar("status", 32)
        val reason = varchar("reason", 32)
        val expires = varchar("expires", 32).nullable()
        val comment = varchar("comment", 255).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun create(serialNumber: String, entry: Entry) = newSuspendedTransaction(Dispatchers.IO) {
        ListTable.insert {
            it[sn] = serialNumber
            it[status] = entry.status
            it[reason] = entry.reason
            it[expires] = entry.expires
            it[comment] = entry.comment
        }[ListTable.id]
    }

    suspend fun query(sn: String) = newSuspendedTransaction(Dispatchers.IO) {
        ListTable.selectAll()
            .where { ListTable.sn eq sn }
            .map { Entry(it[ListTable.status], it[ListTable.reason], it[ListTable.expires], it[ListTable.comment]) }
            .singleOrNull()
    }

    suspend fun count() = newSuspendedTransaction(Dispatchers.IO) {
        ListTable.selectAll().count()
    }
}

@Serializable
data class Entry(
    val status: String,
    val reason: String,
    val expires: String? = null,
    val comment: String? = null,
)

fun checkRevocation(sn: String): String? = runCatching {
    cache.getIfPresent(sn)?.run { return@runCatching this }

    // fallback to dbCache
    CoroutineScope(Dispatchers.IO).future {
        DbRevocationList.query(sn)
    }.get()?.run {
        return@runCatching this.reason
    }

    // cache missed, query online
    CoroutineScope(Dispatchers.IO).future {
        client.get("https://android.googleapis.com/attestation/status?" + System.currentTimeMillis()).let {
            val revocationList: RevocationList = Json.decodeFromString(it.body())
            revocationList.entries[sn]?.also {
                CoroutineScope(Dispatchers.IO).launch {
                    cache.put(sn, it.reason)
                    DbRevocationList.create(sn, it)
                }
            }?.reason
        }
    }.get()
}.onFailure {
    println(it.message)
}.getOrNull()
