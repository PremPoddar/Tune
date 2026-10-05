package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DumpParserTest {
    // Android 12+ style lines from `dumpsys audio`.
    private val modern = """
        |  playback activity as reported through PlayerBase:
        |  AudioPlaybackConfiguration piid:79 deviceId:2 type:android.media.AudioTrack u/pid:10187/8762 state:started attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x800 tags= bundle=null session:1489
        |  AudioPlaybackConfiguration piid:87 deviceId:0 type:android.media.AudioTrack u/pid:10203/9120 state:paused attr:AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_SPEECH flags=0x0 tags= bundle=null session:1601
        |  AudioPlaybackConfiguration piid:95 deviceId:0 type:android.media.SoundPool u/pid:1000/1500 state:idle attr:AudioAttributes: usage=USAGE_ASSISTANCE_SONIFICATION session:0
        |  ducked players piids:
    """.trimMargin()

    // Older style with " -- " separators and sessionId.
    private val legacy = """
        |  ID:15 -- type:android.media.MediaPlayer -- u/pid:10155/4321 -- state:started -- attr:AudioAttributes: usage=USAGE_MEDIA -- sessionId:2001
    """.trimMargin()

    @Test fun parsesModernLines() {
        val e = DumpParser.parsePlayback(modern)
        assertThat(e).containsExactly(
            DumpParser.Entry(uid = 10187, session = 1489, started = true),
            DumpParser.Entry(uid = 10203, session = 1601, started = false),
        )
    }

    @Test fun parsesLegacyLines() {
        assertThat(DumpParser.parsePlayback(legacy))
            .containsExactly(DumpParser.Entry(uid = 10155, session = 2001, started = true))
    }

    @Test fun ignoresGarbage() {
        assertThat(DumpParser.parsePlayback("nothing to see\nu/pid:abc state:started")).isEmpty()
    }
}
