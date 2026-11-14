package com.sadra.ai.ai

object PersianText {
    /** Unifies Arabic/Persian letters, converts Persian/Arabic digits to Latin, ZWNJ to space, strips punctuation. */
    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            sb.append(
                when (c) {
                    'ي' -> 'ی'
                    'ك' -> 'ک'
                    '\u200c' -> ' '
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    '،', '؟', '?', '!', '.', ',', '؛', ':' -> ' '
                    else -> c
                }
            )
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private val WORDS = mapOf(
        "صفر" to 0, "یک" to 1, "یه" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
        "شش" to 6, "شیش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9, "ده" to 10,
        "یازده" to 11, "دوازده" to 12, "سیزده" to 13, "چهارده" to 14, "پانزده" to 15,
        "شانزده" to 16, "هفده" to 17, "هجده" to 18, "نوزده" to 19,
        "بیست" to 20, "سی" to 30, "چهل" to 40, "پنجاه" to 50, "شصت" to 60, "هفتاد" to 70, "هشتاد" to 80, "نود" to 90, "صد" to 100
    )

    /** Parses a number (digits or Persian words, e.g. "بیست و پنج") starting at token i. Returns value and next index. */
    fun parseNumber(t: List<String>, i: Int): Pair<Int, Int>? {
        if (i >= t.size) return null
        t[i].toIntOrNull()?.let { return it to (i + 1) }
        val v = WORDS[t[i]] ?: return null
        if (v >= 20 && v % 10 == 0 && i + 2 < t.size && t[i + 1] == "و") {
            val u = WORDS[t[i + 2]]
            if (u != null && u in 1..9) return (v + u) to (i + 3)
        }
        return v to (i + 1)
    }
}
