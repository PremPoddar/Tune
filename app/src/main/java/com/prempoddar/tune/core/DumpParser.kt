package com.prempoddar.tune.core

/**
 * Reads the playback list in `dumpsys audio` output, which names the audio session of every
 * player, including apps that never announce their session.
 */
object DumpParser {

    data class Entry(val uid: Int, val session: Int, val started: Boolean)

    private val uidRe = Regex("""u/pid:(\d+)/\d+""")
    private val stateRe = Regex("""state:(\w+)""")
    private val sessionRe = Regex("""session(?:Id)?:(\d+)""")

    fun parsePlayback(dump: String): List<Entry> = dump.lineSequence().mapNotNull { line ->
        val uid = uidRe.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        val session = sessionRe.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        if (session <= 0) return@mapNotNull null
        val state = stateRe.find(line)?.groupValues?.get(1)
        Entry(uid, session, started = state == "started")
    }.distinct().toList()
}
