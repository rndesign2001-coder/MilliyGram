package org.fenixuz.utils

import android.content.Context
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.R

object ConfirmDialogsPref {
    var confirmSticker = true
    var confirmVoice = true
    var confirmGif = true

    private var sharedPreferences =
        ApplicationLoader.applicationContext.getSharedPreferences("db", Context.MODE_PRIVATE)
    private var editor = sharedPreferences.edit()

    init {
        confirmSticker = sharedPreferences.getBoolean("confirm_sticker", true)
        confirmVoice = sharedPreferences.getBoolean("confirm_voice", true)
        confirmGif = sharedPreferences.getBoolean("confirm_gif", true)
    }

    fun changeConfirmStickerMode() {
        confirmSticker = !confirmSticker
        editor.putBoolean("confirm_sticker", confirmSticker)
        editor.commit()
    }

    fun changeConfirmVoiceMode() {
        confirmVoice = !confirmVoice
        editor.putBoolean("confirm_voice", confirmVoice)
        editor.commit()
    }

    fun changeConfirmGifMode() {
        confirmGif = !confirmGif
        editor.putBoolean("confirm_gif", confirmGif)
        editor.commit()
    }
}