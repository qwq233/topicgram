package top.qwq2333.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.io.File

@Serializable
data class Config(
    val host: String = "localhost",
    val port: Int = 3579,
    val database: String = "test",
    val user: String = "root",
    val password: String = "root",

    val token: String = "null",
    @SerialName("owner_id") val ownerId: Long = 123456L,
    @SerialName("enable_audit") val enableAudit: Boolean = true
)

lateinit var config: Config

fun initConfig() = runCatching {
    config = File("./config.json").readText(Charsets.UTF_8).run {
        Json {
            allowComments = true
        }.decodeFromString(Config.serializer(), this)
    }
    database = Database.connect(
        driver = "org.mariadb.jdbc.Driver",
        url = "jdbc:mariadb://${config.host}:${config.port}/${config.database}",
        user = config.user,
        password = config.password,
    )
}
