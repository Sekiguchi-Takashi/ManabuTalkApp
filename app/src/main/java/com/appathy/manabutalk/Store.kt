package com.appathy.manabutalk

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 学習記録の永続化と分析。外部ライブラリを使わずSharedPreferencesで実装。
 *
 * セクション識別子: "am2"(午前Ⅱ/セキュリティ) / "am1"(午前Ⅰ/共通)
 * 問題ID: "セクション|年度|問番号"
 */
object Store {

    private const val PREF = "sc_store"
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(Date())

    fun qid(section: String, year: String, no: Int) = "$section|$year|$no"

    // ---- 回答の記録 ----
    fun recordAnswer(section: String, year: String, no: Int, correct: Boolean) {
        val e = sp.edit()
        val id = qid(section, year, no)

        // 全体集計
        e.putInt("total_ans", sp.getInt("total_ans", 0) + 1)
        if (correct) e.putInt("total_cor", sp.getInt("total_cor", 0) + 1)

        // セクション別集計
        e.putInt("sec_att_$section", sp.getInt("sec_att_$section", 0) + 1)
        if (correct) e.putInt("sec_cor_$section", sp.getInt("sec_cor_$section", 0) + 1)

        // 年度別集計
        e.putInt("yr_att_${section}_$year", sp.getInt("yr_att_${section}_$year", 0) + 1)
        if (correct) e.putInt("yr_cor_${section}_$year", sp.getInt("yr_cor_${section}_$year", 0) + 1)

        // 問題別集計
        e.putInt("q_att_$id", sp.getInt("q_att_$id", 0) + 1)
        if (correct) e.putInt("q_cor_$id", sp.getInt("q_cor_$id", 0) + 1)
        e.putInt("q_last_$id", if (correct) 1 else 0)

        // 復習キュー(間違えたら追加、正解で除外)
        val wrong = HashSet(sp.getStringSet("wrong_set", HashSet()) ?: HashSet())
        if (correct) wrong.remove(id) else wrong.add(id)
        e.putStringSet("wrong_set", wrong)

        // 忘却曲線ボックス(0-5)。正解でボックス+1、不正解で0へ。
        val box = sp.getInt("box_$id", 0)
        val newBox = if (correct) (box + 1).coerceAtMost(5) else 0
        e.putInt("box_$id", newBox)
        e.putString("due_$id", nextDue(newBox))

        recordStudyDay(e)
        e.apply()
    }

    // ---- 学習日・連続日数 ----
    private fun recordStudyDay(e: SharedPreferences.Editor) {
        val t = today()
        val last = sp.getString("last_day", "")
        if (last == t) return  // 同日は連続数を変えない

        val days = HashSet(sp.getStringSet("days_set", HashSet()) ?: HashSet())
        days.add(t)
        e.putStringSet("days_set", days)

        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
        )
        val streak = if (last == yesterday) sp.getInt("streak", 0) + 1 else 1
        e.putInt("streak", streak)
        e.putInt("best_streak", maxOf(streak, sp.getInt("best_streak", 0)))
        e.putString("last_day", t)
    }

    private fun nextDue(box: Int): String {
        // ボックス0,1,2,3,4,5 → 0,1,3,7,14,30日後
        val add = intArrayOf(0, 1, 3, 7, 14, 30)[box.coerceIn(0, 5)]
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, add) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(c.time)
    }

    // ---- 参照系 ----
    fun totalAnswered() = sp.getInt("total_ans", 0)
    fun totalCorrect() = sp.getInt("total_cor", 0)
    fun overallRate(): Int {
        val a = totalAnswered(); return if (a == 0) 0 else totalCorrect() * 100 / a
    }
    fun sectionAtt(section: String) = sp.getInt("sec_att_$section", 0)
    fun sectionCor(section: String) = sp.getInt("sec_cor_$section", 0)
    fun sectionRate(section: String): Int {
        val a = sectionAtt(section); return if (a == 0) 0 else sectionCor(section) * 100 / a
    }
    fun yearAtt(section: String, year: String) = sp.getInt("yr_att_${section}_$year", 0)
    fun yearCor(section: String, year: String) = sp.getInt("yr_cor_${section}_$year", 0)
    fun yearRate(section: String, year: String): Int {
        val a = yearAtt(section, year); return if (a == 0) 0 else yearCor(section, year) * 100 / a
    }

    // 問題ID単位の集計(分野別分析で使用)
    fun qAtt(id: String) = sp.getInt("q_att_$id", 0)
    fun qCor(id: String) = sp.getInt("q_cor_$id", 0)

    fun streak() = sp.getInt("streak", 0)
    fun bestStreak() = sp.getInt("best_streak", 0)
    fun studyDays() = (sp.getStringSet("days_set", HashSet()) ?: HashSet()).size
    fun answeredToday(): Boolean = sp.getString("last_day", "") == today()

    fun wrongIds(): List<String> =
        (sp.getStringSet("wrong_set", HashSet()) ?: HashSet()).toList()
    fun wrongCount() = wrongIds().size

    /** 復習期限が今日以前になっているカードID(一度でも解いたもの) */
    fun dueIds(): List<String> {
        val t = today()
        val res = ArrayList<String>()
        for ((k, _) in sp.all) {
            if (k.startsWith("due_")) {
                val due = sp.getString(k, null) ?: continue
                if (due <= t) res.add(k.removePrefix("due_"))
            }
        }
        return res
    }

    /** よく間違える問題(正答率が低い順)。最低2回解いたもの。 */
    fun weakQuestionIds(limit: Int): List<String> {
        val list = ArrayList<Triple<String, Int, Int>>() // id, att, cor
        for ((k, v) in sp.all) {
            if (k.startsWith("q_att_")) {
                val id = k.removePrefix("q_att_")
                val att = (v as? Int) ?: 0
                if (att >= 2) list.add(Triple(id, att, sp.getInt("q_cor_$id", 0)))
            }
        }
        list.sortBy { (it.third.toDouble() / it.second) }
        return list.take(limit).map { it.first }
    }

    /** 単語帳の自己評価。正答率統計には影響させず、忘却曲線のみ更新。 */
    fun recordFlash(id: String, remembered: Boolean) {
        val e = sp.edit()
        val box = sp.getInt("box_$id", 0)
        val newBox = if (remembered) (box + 1).coerceAtMost(5) else 0
        e.putInt("box_$id", newBox)
        e.putString("due_$id", nextDue(newBox))
        recordStudyDay(e)
        e.apply()
    }

    fun resetAll() {
        sp.edit().clear().apply()
    }

    // 合格可能性の目安(午前Ⅱの直近的な正答率を基準に、60%合格ラインで簡易推定)
    fun passEstimate(): Int {
        val am2 = sectionRate("am2")
        val am1 = sectionRate("am1")
        val att = sectionAtt("am2") + sectionAtt("am1")
        if (att < 10) return -1 // データ不足
        // 午前Ⅱを主、午前Ⅰを従として合格ライン60%に対する到達度
        val score = (am2 * 0.7 + am1 * 0.3)
        return (score / 60.0 * 100).toInt().coerceIn(0, 100)
    }
}
