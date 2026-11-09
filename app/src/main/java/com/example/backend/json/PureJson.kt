package com.example.backend.json

/**
 * Pure-Kotlin JSON DOM (`JSONObject` & `JSONArray`) with zero dependency on Android's
 * stubbed `android.jar` `org.json.*` classes so that Innertube request serialization
 * and response parsing execute identically on Android devices and in local JVM unit tests.
 */
class JSONObject() {
    private val map = LinkedHashMap<String, Any?>()

    constructor(rawJson: String) : this() {
        val parsed = SimpleJsonTokenizer(rawJson).parseValue()
        if (parsed is JSONObject) {
            this.map.putAll(parsed.map)
        }
    }

    fun put(key: String, value: Any?): JSONObject {
        map[key] = value
        return this
    }

    fun optJSONObject(key: String): JSONObject? = map[key] as? JSONObject

    fun optJSONArray(key: String): JSONArray? = map[key] as? JSONArray

    fun optString(key: String, fallback: String = ""): String {
        val v = map[key] ?: return fallback
        return v.toString()
    }

    fun optBoolean(key: String, fallback: Boolean = false): Boolean {
        return when (val v = map[key]) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            else -> fallback
        }
    }

    fun optLong(key: String, fallback: Long = 0L): Long {
        return when (val v = map[key]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: fallback
            else -> fallback
        }
    }

    override fun toString(): String {
        return buildString {
            append('{')
            var first = true
            for ((k, v) in map) {
                if (!first) append(',')
                first = false
                append('"').append(escapeJson(k)).append("\":")
                append(valueToJsonString(v))
            }
            append('}')
        }
    }
}

class JSONArray {
    private val list = ArrayList<Any?>()

    fun put(value: Any?): JSONArray {
        list.add(value)
        return this
    }

    fun length(): Int = list.size

    fun optJSONObject(index: Int): JSONObject? =
        if (index in 0 until list.size) list[index] as? JSONObject else null

    fun optJSONArray(index: Int): JSONArray? =
        if (index in 0 until list.size) list[index] as? JSONArray else null

    fun optString(index: Int, fallback: String = ""): String =
        if (index in 0 until list.size) list[index]?.toString() ?: fallback else fallback

    override fun toString(): String {
        return buildString {
            append('[')
            for (i in list.indices) {
                if (i > 0) append(',')
                append(valueToJsonString(list[i]))
            }
            append(']')
        }
    }
}

private fun valueToJsonString(value: Any?): String = when (value) {
    null -> "null"
    is JSONObject -> value.toString()
    is JSONArray -> value.toString()
    is Boolean -> value.toString()
    is Number -> value.toString()
    else -> "\"${escapeJson(value.toString())}\""
}

private fun escapeJson(s: String): String = buildString(s.length) {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

private class SimpleJsonTokenizer(private val src: String) {
    private var pos = 0

    fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) return null
        return when (src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            else -> parseNumberOrLiteral()
        }
    }

    private fun parseObject(): JSONObject {
        val obj = JSONObject()
        pos++ // skip '{'
        skipWhitespace()
        if (pos < src.length && src[pos] == '}') {
            pos++
            return obj
        }
        while (pos < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            if (pos < src.length && src[pos] == ':') pos++
            val value = parseValue()
            obj.put(key, value)
            skipWhitespace()
            if (pos < src.length && src[pos] == ',') {
                pos++
            } else if (pos < src.length && src[pos] == '}') {
                pos++
                break
            } else {
                break
            }
        }
        return obj
    }

    private fun parseArray(): JSONArray {
        val arr = JSONArray()
        pos++ // skip '['
        skipWhitespace()
        if (pos < src.length && src[pos] == ']') {
            pos++
            return arr
        }
        while (pos < src.length) {
            arr.put(parseValue())
            skipWhitespace()
            if (pos < src.length && src[pos] == ',') {
                pos++
            } else if (pos < src.length && src[pos] == ']') {
                pos++
                break
            } else {
                break
            }
        }
        return arr
    }

    private fun parseString(): String {
        if (pos < src.length && src[pos] == '"') pos++
        val sb = StringBuilder()
        while (pos < src.length) {
            val c = src[pos++]
            if (c == '"') break
            if (c == '\\' && pos < src.length) {
                when (val esc = src[pos++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pos + 4 <= src.length) {
                            val hex = src.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", pos)) {
            pos += 4
            true
        } else {
            pos += 5
            false
        }
    }

    private fun parseNull(): Any? {
        pos += 4
        return null
    }

    private fun parseNumberOrLiteral(): Any {
        val start = pos
        while (pos < src.length && src[pos] !in charArrayOf(',', '}', ']', ' ', '\n', '\r', '\t')) {
            pos++
        }
        val token = src.substring(start, pos)
        return token.toLongOrNull() ?: token.toDoubleOrNull() ?: token
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) {
            pos++
        }
    }
}
