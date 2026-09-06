package com.musicplayer.app.core.util

import com.musicplayer.app.core.model.LrcLine

object LrcParser {

    private val timeTagRegex = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:\\.(\\d{1,3}))?]")

    fun parse(lrc: String): List<LrcLine> {
        if (lrc.isBlank()) return emptyList()

        val result = mutableListOf<LrcLine>()
        lrc.lineSequence().forEach { raw ->
            var rest = raw
            val times = mutableListOf<Long>()
            while (true) {
                val match = timeTagRegex.find(rest) ?: break
                val minutes = match.groupValues[1].toLong()
                val seconds = match.groupValues[2].toLong()
                val fraction = match.groupValues[3]
                val millis = if (fraction.isEmpty()) {
                    0L
                } else {
                    fraction.padEnd(3, '0').take(3).toLong()
                }
                times += minutes * 60_000L + seconds * 1_000L + millis
                rest = rest.substring(match.range.last + 1)
            }

            if (times.isEmpty()) return@forEach
            val text = rest.trim()
            if (text.isEmpty()) return@forEach

            times.forEach { result += LrcLine(timeMs = it, text = text) }
        }

        return result.sortedBy { it.timeMs }
    }
}