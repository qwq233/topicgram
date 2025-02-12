package top.qwq2333

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import top.qwq2333.data.config


fun Bot.Builder.configureBot() {
    token = config.token
    logLevel = LogLevel.All()

    dispatch {
        configureCommand()
    }
}

fun Dispatcher.configureCommand() {
    command("start") {
        bot.sendMessage(
            chatId = ChatId.fromId(message.chat.id),
            text = "2333"
        ).onSuccess {

        }.onError {

        }
    }

    command("bind") {

    }
}