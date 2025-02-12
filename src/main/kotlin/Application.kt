package top.qwq2333

import com.github.kotlintelegrambot.bot
import nl.adaptivity.xmlutil.serialization.XML
import org.bouncycastle.jce.provider.BouncyCastleProvider
import top.qwq2333.data.AndroidAttestation
import top.qwq2333.data.GOOGLE_ROOT_PUBLIC_KEY
import top.qwq2333.data.checkRevocation
import top.qwq2333.data.initConfig
import java.io.File
import java.security.Security


fun main() {
    initConfig()

    Security.addProvider(BouncyCastleProvider())
    val content = File("./testbox.xml").readLines().run {
        StringBuilder().also { builder ->
            forEach {
                builder.append(it)
            }
        }.toString()
    }

    val bot = bot {
        configureBot()
    }
    bot.startPolling()

    val keybox = XML.decodeFromString(AndroidAttestation.serializer(), content)
    println(keybox.keyBox.keys.first().chain.certificates.last().x509.publicKey.encoded.contentEquals(GOOGLE_ROOT_PUBLIC_KEY))
    var time = System.currentTimeMillis()
    println(checkRevocation("6e5998d0c8e7a361c662166c8c5f8105"))
    println("time: ${System.currentTimeMillis() - time}")

    time = System.currentTimeMillis()
    println(checkRevocation("23333333"))
    println("time: ${System.currentTimeMillis() - time}")

}
