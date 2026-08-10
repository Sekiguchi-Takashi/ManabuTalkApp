package com.appathy.manabutalk

/**
 * 解説文から「答えに近づくヒント」を自動生成する。
 * 方針: 正解の選択肢の語・正解の記号(ア〜エ)を伏せ、着眼点や対比軸だけを残す。
 * 解説の先頭文(核心の理由)を中心に、答えを直接言わない形へ加工する。
 */
object Hint {

    private val letters = listOf("ア", "イ", "ウ", "エ")

    fun of(q: QuestionData.Question): String {
        val exp = q.explanation
        // 先頭文を基本にする(短ければ2文目まで)
        val sentences = exp.split("。").map { it.trim() }.filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return "選択肢を、問われている定義や対比軸に照らして一つずつ吟味しましょう。"
        var hint = sentences[0]
        if (hint.length < 16 && sentences.size >= 2) hint += "。" + sentences[1]

        // 正解の記号(ア〜エ)を、選択肢参照の文脈でだけ伏せる
        val letter = q.answerText.trim()
        if (letter in letters) {
            for (suf in listOf("は", "（", "(", "が", "の", "、", "で", "を", "も")) {
                hint = hint.replace(letter + suf, "□" + suf)
            }
        }

        // 正解の選択肢が短い語なら伏せる(長文の選択肢は伏せない)
        val ans = q.choices?.getOrNull(q.answerIndex)
        if (ans != null && ans.length in 1..16 && !ans.contains("。") && !ans.contains("、")) {
            hint = hint.replace(ans, "□□")
        }
        // 長文の選択肢でも、先頭の語(名詞句)が特徴的なら伏せる
        if (ans != null) {
            val head = ans.takeWhile { it !in "、。がはをにへとでも談（(」や・" }
            if (head.length in 4..16) hint = hint.replace(head, "□□")
        }

        hint = hint.trim().trimEnd('、', '。')
        // ほぼ伏字だけになった場合の保険
        val masked = hint.count { it == '□' }
        if (hint.replace("□", "").length < 6 || masked >= 6) {
            return "問われている定義・目的や、似た用語との違い(対比軸)に注目して絞り込みましょう。"
        }
        return hint + (if (sentences.size > 1) "…" else "")
    }
}
