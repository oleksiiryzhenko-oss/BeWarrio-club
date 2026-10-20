package com.katadojo.app.utils

import android.media.AudioManager
import android.media.ToneGenerator

/** Short timer beeps through the media stream. No permissions needed. */
class TonePlayer {
    private val generator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    } catch (e: RuntimeException) {
        null
    }

    fun beep() {
        generator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
    }

    fun done() {
        generator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 500)
    }

    fun release() {
        generator?.release()
    }
}
