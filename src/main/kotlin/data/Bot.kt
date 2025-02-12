package top.qwq2333.data

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatPermissions
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.files.ChatPhoto
import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName as Name

object Messages {
    init {
        transaction(database) {
            SchemaUtils.create(MessagesTable)
        }
    }

    object MessagesTable : Table("messages") {
        val id = long("id").autoIncrement()

        val message_id = long("message_id")
        val chat = long("chat_id")

        val senderChatId = long("sender_chat_id").nullable()
        val from = long("from").nullable()
        val date = long("date")
        val forwardInfo = varchar("forward_info", 255).nullable()

        /**
         * format: `message_id,chat_id,thread_id`
         *
         * `thread_id = -1` if not in a thread
         */
        val replyToMessage = varchar("reply_to_message", 32).nullable()
        val messageThreadId = long("message_thread_id").nullable()

        val textWithEntities = text("text").nullable()
        val captionWithEntities = text("caption").nullable()
        val document = varchar("document", 255).nullable()
        val mediaGroupId = varchar("media_group_id", 32).nullable()

        val isDeleted = bool("is_deleted").default(false)
        val edit_history = text("edit_history").default("[]")

        val raw = text("raw")

        override val primaryKey = PrimaryKey(id)
    }

    @Serializable
    data class TextWithEntities(
        @Name("text") val text: String,
        @Name("entities") val entities: String,
    )

    @Serializable
    data class ForwardInfo(
        @Name("forward_from") val from: Long,
        @Name("forward_from_chat") val fromChat: Long,
        @Name("forward_from_message_id") val messageId: Long,
        @Name("forward_signature") val signature: String,
        @Name("forward_sender_name") val senderName: String,
        @Name("forward_date") val date: Int,
    )

    @Serializable
    data class Document(
        @Name("document") val document: String? = null,
        @Name("audio") val audio: String? = null,
        @Name("animation") val animation: String? = null,
        @Name("photo") val photo: String? = null,
        @Name("sticker") val sticker: String? = null,
        @Name("video") val video: String? = null,
        @Name("voice") val voice: String? = null,
        @Name("contact") val contact: String? = null,
    )

    suspend fun create(message: Message) {
        MessagesTable.insert {
            it[message_id] = message.messageId

            it[senderChatId] = message.senderChat?.id
            it[from] = message.from?.id
            it[date] = message.date
            it[forwardInfo] = ForwardInfo(
                message.forwardFrom?.id ?: 0,
                message.forwardFromChat?.id ?: 0,
                message.forwardFromMessageId?.toLong() ?: 0,
                message.forwardSignature ?: "",
                message.forwardSenderName ?: "",
                message.forwardDate ?: 0
            ).run {
                Json.encodeToString(this)
            }

            it[messageThreadId] = message.messageThreadId
            it[textWithEntities] = TextWithEntities(
                message.text ?: "",
                message.entities?.run { Gson().toJson(this) } ?: ""
            ).run {
                Json.encodeToString(this)
            }

            it[captionWithEntities] = TextWithEntities(
                message.caption ?: "",
                message.captionEntities?.run { Gson().toJson(this) } ?: ""
            ).run {
                Json.encodeToString(this)
            }

            it[replyToMessage] = message.replyToMessage?.run {
                "$messageId,${chat.id},$messageThreadId"
            }

            it[document] = Document(
                message.document?.run { Gson().toJson(this) },
                message.audio?.run { Gson().toJson(this) },
                message.animation?.run { Gson().toJson(this) },
                message.photo?.run { Gson().toJson(this) },
                message.sticker?.run { Gson().toJson(this) },
                message.video?.run { Gson().toJson(this) },
                message.voice?.run { Gson().toJson(this) },
                message.contact?.run { Gson().toJson(this) },
            ).run {
                Json.encodeToString(this)
            }

            it[mediaGroupId] = message.mediaGroupId
            it[raw] = Gson().toJson(message)
        }
    }

    suspend fun query(messageId: Long, chatId: Long): Nothing = newSuspendedTransaction(Dispatchers.IO) {
        TODO()
    }

    suspend fun edit(messageId: Long, chatId: Long, message: Message) = newSuspendedTransaction {
        val msg = MessagesTable.selectAll().where { (MessagesTable.id eq messageId) and (MessagesTable.chat eq chatId) }.single()[MessagesTable.edit_history]
        MessagesTable.update({ (MessagesTable.id eq messageId) and (MessagesTable.chat eq chatId) }) { update ->
            update[edit_history] = msg.run {
                Gson().fromJson(this, List::class.java).apply {
                    mutableListOf(Gson().toJson(message)).apply {
                        addAll(this)
                    }
                }.run {
                    Gson().toJson(this)
                }
            }
        }
    }
}

object Chats {
    init {
        transaction(database) {
            SchemaUtils.create(ChatTable)
        }
    }

    object ChatTable : Table("chats") {
        val id = long("id")
        val type = varchar("type", length = 16)

        val title = text("title").nullable()
        val username = text("username").nullable()
        val firstName = text("first_name").nullable()
        val lastName = text("last_name").nullable()

        val photo = text("photo").nullable()
        val bio = text("bio").nullable()
        val description = text("description").nullable()
        val permissions = text("permissions").nullable()
        val slowModeDelay = integer("slow_mode_delay").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun create(chat: Chat) {
        ChatTable.insert {
            it[id] = chat.id
            it[type] = chat.type
            it[title] = chat.title
            it[username] = chat.username
            it[firstName] = chat.firstName
            it[lastName] = chat.lastName
            it[photo] = Gson().toJson(chat.photo)
            it[bio] = chat.bio
            it[description] = chat.description
            it[permissions] = Gson().toJson(chat.permissions)
            it[slowModeDelay] = chat.slowModeDelay

        }
    }

    suspend fun query(chatId: Long) = newSuspendedTransaction(Dispatchers.IO) {
        ChatTable.selectAll().where(ChatTable.id eq chatId).map {
            Chat(
                id = it[ChatTable.id],
                type = it[ChatTable.type],
                title = it[ChatTable.title],
                username = it[ChatTable.username],
                firstName = it[ChatTable.firstName],
                lastName = it[ChatTable.lastName],
                photo = Gson().fromJson(it[ChatTable.photo], ChatPhoto::class.java),
                bio = it[ChatTable.bio],
                description = it[ChatTable.description],
                permissions = Gson().fromJson(it[ChatTable.permissions], ChatPermissions::class.java),
                slowModeDelay = it[ChatTable.slowModeDelay],
            )
        }
    }
}

object Bots {
    init {
        transaction(database) {
            SchemaUtils.create(BotTable)
        }
    }

    object BotTable : Table("bots") {
        val botId = long("bot_id")
        val token = varchar("token", 64)
        val botOwner = long("bot_owner") references Chats.ChatTable.id
        val manageGroupId = long("manage_group_id").default(-1) references Chats.ChatTable.id

        val isDeleted = bool("is_deleted").default(false)

        override val primaryKey = PrimaryKey(botId)
    }

    suspend fun create(info: Pair<String, Long>) {
        BotTable.insert {
            it[botId] = info.first.split(":")[0].toLong()
            it[token] = info.first
            it[botOwner] = info.second
        }
    }

    suspend fun query(botId: Long) = newSuspendedTransaction(Dispatchers.IO) {
        BotTable.selectAll().where(BotTable.botId eq botId).map {
            Pair(it[BotTable.token], it[BotTable.botOwner])
        }
    }

    suspend fun delete(botId: Long) = newSuspendedTransaction(Dispatchers.IO) {
        if (config.enableAudit) {
            BotTable.update({ BotTable.botId eq botId }) {
                it[isDeleted] = true
            }
        } else {
            BotTable.deleteWhere { BotTable.botId eq botId }
            Threads.ThreadTable.deleteWhere { bot eq botId }
        }
    }
}

object Threads {
    init {
        transaction(database) {
            SchemaUtils.create(ThreadTable)
        }
    }

    val cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, Long>()

    object ThreadTable : Table("threads") {
        val id = integer("id").autoIncrement()

        val bot = long("bot_id") references Bots.BotTable.botId
        val chatId = long("chat_id") references Chats.ChatTable.id
        val threadId = long("thread_id")
        val belongsTo = long("belongs_to") references Chats.ChatTable.id

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun create(botId: Long, chat: Long, thread: Long, belongsTo: Long) = newSuspendedTransaction(Dispatchers.IO) {
        ThreadTable.insert {
            it[bot] = botId
            it[chatId] = chatId
            it[threadId] = threadId
            it[this.belongsTo] = belongsTo
        }
    }

    suspend fun query(botId: Long, chat: Long, thread: Long) = newSuspendedTransaction(Dispatchers.IO) {
        cache.getIfPresent("$botId,$chat,$thread")?.let { return@newSuspendedTransaction it }

        ThreadTable.selectAll().where {
            (ThreadTable.bot eq botId) and
                    (ThreadTable.chatId eq chat) and
                    (ThreadTable.threadId eq thread)
        }.map {
            it[ThreadTable.belongsTo]
        }.single().also {
            cache.put("$botId,$chat,$thread", it)
        }
    }

    suspend fun query(botId: Long, belongsTo: Long) = newSuspendedTransaction(Dispatchers.IO) {
        cache.asMap().filter { it.value == belongsTo }.keys.single { it.contains(botId.toString()) }.run {
            val (_, chat, thread) = split(",")
            Pair(chat.toLong(), thread.toLong())
        }

        ThreadTable.selectAll().where { (ThreadTable.belongsTo eq belongsTo) and (ThreadTable.bot eq botId) }.map { res ->
            Pair(res[ThreadTable.chatId], res[ThreadTable.threadId]).also {
                cache.put("$botId,${it.first},${it.second}", belongsTo)
            }
        }
    }
}