package com.example.utils

object HindiNumberToWords {
    private val ones = arrayOf(
        "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
        "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
        "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
        "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
        "इकतालीस", "बयालीस", "तैंतालीस", "चौवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
        "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ",
        "इकसठ", "बासठ", "तिरेसठ", "चौंसठ", "पैंसठ", "छियासठ", "सड़सठ", "अड़सठ", "उनहत्तर", "सत्तर",
        "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उनासी", "अस्सी",
        "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी", "नब्बे",
        "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे"
    )

    fun convert(amount: Double): String {
        val n = kotlin.math.floor(amount).toLong()
        if (n <= 0) return "शून्य रुपये मात्र"
        val words = convertInternal(n).trim()
        return "$words रुपये मात्र"
    }

    private fun convertInternal(n: Long): String {
        if (n < 100) {
            return ones.getOrElse(n.toInt()) { "" }
        }
        if (n < 1000) {
            val h = (n / 100).toInt()
            val rem = n % 100
            val hWord = "${ones.getOrElse(h) { "" }} सौ"
            return if (rem > 0) "$hWord ${convertInternal(rem)}" else hWord
        }
        if (n < 100000) {
            val th = n / 1000
            val rem = n % 1000
            val thWord = "${convertInternal(th)} हजार"
            return if (rem > 0) "$thWord ${convertInternal(rem)}" else thWord
        }
        if (n < 10000000) {
            val lk = n / 100000
            val rem = n % 100000
            val lkWord = "${convertInternal(lk)} लाख"
            return if (rem > 0) "$lkWord ${convertInternal(rem)}" else lkWord
        }
        val cr = n / 10000000
        val rem = n % 10000000
        val crWord = "${convertInternal(cr)} करोड़"
        return if (rem > 0) "$crWord ${convertInternal(rem)}" else crWord
    }
}
