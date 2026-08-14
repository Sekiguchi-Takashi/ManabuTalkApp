package com.appathy.manabutalk

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/**
 * 情報処理安全確保支援士 学習アプリ。
 * 5タブ構成(ホーム/学習/AI/分析/マイページ)。過去問データはQuestionDataを流用。
 * XMLレイアウト不使用・プログラマティックKotlin・外部依存なし。
 */
class MainActivity : Activity() {

    // ---- カラーパレット(セキュリティ系ダーク) ----
    private val cBg = 0xFF0E1116.toInt()
    private val cCard = 0xFF19212C.toInt()
    private val cCard2 = 0xFF222C3A.toInt()
    private val cAccent = 0xFF2EA6FF.toInt()
    private val cGreen = 0xFF3DD68C.toInt()
    private val cRed = 0xFFF2555A.toInt()
    private val cGold = 0xFFE6B450.toInt()
    private val cText = 0xFFECF1F8.toInt()
    private val cSub = 0xFF97A6B6.toInt()

    private lateinit var content: FrameLayout
    private lateinit var tabBar: LinearLayout
    private var currentTab = 0
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(cBg)

        content = FrameLayout(this)
        content.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        root.addView(content)

        tabBar = buildTabBar()
        root.addView(tabBar)

        setContentView(root)
        showTab(0)
    }

    // ============================================================
    // 共通UIビルダー
    // ============================================================

    private fun rounded(color: Int, radius: Int = 14): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radius).toFloat()
        return g
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        return t
    }

    private fun spacer(h: Int): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(h))
        return v
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = rounded(cCard)
        c.setPadding(dp(16), dp(16), dp(16), dp(16))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        c.layoutParams = lp
        return c
    }

    private fun bigButton(label: String, sub: String?, bg: Int, onClick: () -> Unit): View {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.VERTICAL
        b.background = rounded(bg)
        b.setPadding(dp(18), dp(16), dp(18), dp(16))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        b.layoutParams = lp
        b.addView(tv(label, 17f, Color.WHITE, true))
        if (sub != null) {
            val s = tv(sub, 12.5f, 0xFFE7F0FA.toInt())
            s.setPadding(0, dp(3), 0, 0)
            b.addView(s)
        }
        b.setOnClickListener { onClick() }
        return b
    }

    private fun listButton(label: String, sub: String? = null, onClick: () -> Unit): View {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.HORIZONTAL
        b.gravity = Gravity.CENTER_VERTICAL
        b.background = rounded(cCard2)
        b.setPadding(dp(16), dp(14), dp(16), dp(14))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        b.layoutParams = lp
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        col.addView(tv(label, 15.5f, cText, true))
        if (sub != null) col.addView(tv(sub, 12f, cSub))
        b.addView(col)
        b.addView(tv("›", 20f, cSub))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun pill(text: String, color: Int): TextView {
        val t = tv(text, 12f, Color.WHITE, true)
        t.background = rounded(color, 20)
        t.setPadding(dp(10), dp(4), dp(10), dp(4))
        return t
    }

    /** 画面の骨組み。ScrollView+縦LinearLayout。backがあれば戻るボタンを上部に置く。 */
    private fun screen(title: String, back: (() -> Unit)? = null, build: (LinearLayout) -> Unit): View {
        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.setBackgroundColor(cBg)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(24))
        if (back != null) {
            val bk = tv("‹ もどる", 14f, cAccent, true)
            bk.setPadding(0, 0, 0, dp(10))
            bk.setOnClickListener { back() }
            col.addView(bk)
        }
        col.addView(tv(title, 22f, cText, true))
        col.addView(spacer(12))
        build(col)
        sv.addView(col)
        return sv
    }

    private fun setContent(v: View) {
        handler.removeCallbacksAndMessages(null)
        content.removeAllViews()
        content.addView(v, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    // ============================================================
    // ボトムタブ
    // ============================================================
    private val tabLabels = listOf("ホーム", "学習", "AI", "分析", "マイ")

    private fun buildTabBar(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setBackgroundColor(0xFF141C26.toInt())
        bar.setPadding(0, dp(6), 0, dp(6))
        for (i in tabLabels.indices) {
            val item = tv(tabLabels[i], 12.5f, cSub, true)
            item.gravity = Gravity.CENTER
            item.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            item.setPadding(0, dp(8), 0, dp(8))
            item.setOnClickListener { showTab(i) }
            bar.addView(item)
        }
        return bar
    }

    private fun refreshTabHighlight() {
        for (i in tabLabels.indices) {
            (tabBar.getChildAt(i) as TextView).setTextColor(if (i == currentTab) cAccent else cSub)
        }
    }

    private fun showTab(i: Int) {
        currentTab = i
        refreshTabHighlight()
        when (i) {
            0 -> showHome()
            1 -> showStudyHome()
            2 -> showAiHome()
            3 -> showAnalysis()
            4 -> showMyPage()
        }
    }

    // ============================================================
    // タブ0: ホーム
    // ============================================================
    private fun showHome() {
        val greet = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "おはようございます"
            in 11..17 -> "こんにちは"
            else -> "こんばんは"
        }
        setContent(screen(greet) { col ->
            // ステータスカード
            val c = card()
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(statBlock("連続", "${Store.streak()}日"))
            row.addView(statBlock("今日", if (Store.answeredToday()) "学習済" else "未学習"))
            row.addView(statBlock("正答率", "${Store.overallRate()}%"))
            c.addView(row)
            col.addView(c)

            col.addView(tv("今日のメニュー", 16f, cText, true))
            col.addView(spacer(8))
            col.addView(bigButton("クイック学習", "ランダム10問で腕試し", cAccent) { startRandom(10) })
            col.addView(bigButton("復習する", reviewSub(), 0xFF7A5AF8.toInt()) { showReview() })
            col.addView(bigButton("模試に挑戦", "1年度分を通しで採点", 0xFF15A6A0.toInt()) { showMockPick() })

            // おすすめ(弱点分野 → なければ弱点セクション)
            val wcat = weakestCategory()
            if (wcat != null) {
                col.addView(spacer(4))
                val rec = card()
                rec.addView(tv("AIのおすすめ", 13f, cGold, true))
                rec.addView(spacer(4))
                rec.addView(tv("「${wcat.first}」の正答率が${wcat.second}%。ここを集中的に練習しましょう。", 14f, cText))
                rec.addView(spacer(8))
                rec.addView(listButton("この分野を集中演習する") { startCategoryDrill(wcat.first) })
                col.addView(rec)
            } else {
                val weak = weakestSection()
                if (weak != null) {
                    col.addView(spacer(4))
                    val rec = card()
                    rec.addView(tv("AIのおすすめ", 13f, cGold, true))
                    rec.addView(spacer(4))
                    rec.addView(tv(weak.second, 14f, cText))
                    rec.addView(spacer(8))
                    rec.addView(listButton("この分野を練習する") {
                        startRandomPool(sectionPool(weak.first), weak.first)
                    })
                    col.addView(rec)
                }
            }
        })
    }

    private fun statBlock(label: String, value: String): View {
        val b = LinearLayout(this)
        b.orientation = LinearLayout.VERTICAL
        b.gravity = Gravity.CENTER
        b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        b.addView(tv(value, 20f, cAccent, true).apply { gravity = Gravity.CENTER })
        b.addView(tv(label, 12f, cSub).apply { gravity = Gravity.CENTER })
        return b
    }

    private fun reviewSub(): String {
        val n = Store.wrongCount()
        return if (n == 0) "間違えた問題がここに溜まります" else "間違えた問題が${n}問"
    }

    private fun weakestSection(): Pair<String, String>? {
        val am2 = Store.sectionAtt("am2")
        val am1 = Store.sectionAtt("am1")
        if (am2 + am1 < 5) return null
        val r2 = Store.sectionRate("am2")
        val r1 = Store.sectionRate("am1")
        return if (am1 > 0 && (am2 == 0 || r1 <= r2))
            "am1" to "午前Ⅰ(共通)の正答率が${r1}%。ここを重点的に。"
        else
            "am2" to "午前Ⅱ(セキュリティ)の正答率が${r2}%。ここを重点的に。"
    }

    /** 分野別の[試行数,正解数]を全問走査で集計。 */
    private fun categoryStats(): LinkedHashMap<String, IntArray> {
        val cats = LinkedHashMap<String, IntArray>()
        for (c in Category.all) cats[c] = intArrayOf(0, 0)
        fun acc(section: String, q: QuestionData.Question) {
            val id = Store.qid(section, q.year, q.no)
            val a = Store.qAtt(id)
            if (a > 0) {
                val arr = cats[Category.categoryOf(q)]!!
                arr[0] += a; arr[1] += Store.qCor(id)
            }
        }
        for (q in QuestionData.am2ByYear.values.flatten()) acc("am2", q)
        for (q in QuestionData.am1ByYear.values.flatten()) acc("am1", q)
        return cats
    }

    /** 3回以上挑戦した分野のうち最も正答率が低いもの。 */
    private fun weakestCategory(): Pair<String, Int>? {
        var best: Pair<String, Int>? = null
        for ((cat, arr) in categoryStats()) {
            if (arr[0] >= 3) {
                val r = arr[1] * 100 / arr[0]
                if (best == null || r < best!!.second) best = cat to r
            }
        }
        return best
    }

    private fun startCategoryDrill(cat: String) {
        val pool = Category.pool(cat)
        if (pool.isEmpty()) return
        drillNext(pool, cat)
    }

    private fun drillNext(pool: List<Pair<String, QuestionData.Question>>, cat: String) {
        val (section, q) = pool.random()
        showQuestion(section, q, "分野: $cat", { showAnalysis() }) { drillNext(pool, cat) }
    }

    // ============================================================
    // タブ1: 学習
    // ============================================================
    private fun showStudyHome() {
        setContent(screen("学習") { col ->
            col.addView(sectionHeader("問題演習"))
            col.addView(listButton("午前Ⅱ (セキュリティ)", "${countQ("am2")}問 / ${QuestionData.am2ByYear.size}年度") {
                showYearList("am2")
            })
            col.addView(listButton("午前Ⅰ (共通)", "${countQ("am1")}問 / ${QuestionData.am1ByYear.size}年度") {
                showYearList("am1")
            })
            col.addView(listButton("午後 (記述式・公式PDF)", "${QuestionData.pmExams.size}回分") {
                showPmList()
            })
            col.addView(spacer(6))
            col.addView(sectionHeader("インプット"))
            col.addView(listButton("知識マップ", "全体像と学習順序を図で把握") { showKnowledgeMap() })
            col.addView(listButton("単語帳", "問題で覚える一問一答フラッシュカード") { showFlashcards() })
            col.addView(listButton("用語辞典", "${Glossary.terms.size}語を検索") { showGlossary() })
        })
    }

    private fun sectionHeader(t: String): View {
        val v = tv(t, 14f, cGold, true)
        v.setPadding(dp(2), dp(4), 0, dp(8))
        return v
    }

    private fun countQ(section: String): Int =
        mapOf("am2" to QuestionData.am2ByYear, "am1" to QuestionData.am1ByYear)[section]!!
            .values.sumOf { it.size }

    private fun sectionMap(section: String) =
        if (section == "am2") QuestionData.am2ByYear else QuestionData.am1ByYear

    private fun sectionLabel(section: String) = if (section == "am2") "午前Ⅱ" else "午前Ⅰ"

    private fun sectionPool(section: String): List<QuestionData.Question> =
        sectionMap(section).values.flatten()

    private fun showYearList(section: String) {
        setContent(screen("${sectionLabel(section)} - 年度を選ぶ", back = { showStudyHome() }) { col ->
            col.addView(bigButton("全年度からランダム", "${countQ(section)}問からランダム出題", cAccent) {
                startRandomPool(sectionPool(section), section)
            })
            col.addView(spacer(4))
            for ((year, list) in sectionMap(section)) {
                val rate = Store.yearRate(section, year)
                val att = Store.yearAtt(section, year)
                val sub = if (att == 0) "${list.size}問 ・ 未挑戦" else "${list.size}問 ・ 正答率${rate}%"
                col.addView(listButton(year, sub) { showYearMenu(section, year, list) })
            }
        })
    }

    private fun showYearMenu(section: String, year: String, list: List<QuestionData.Question>) {
        setContent(screen("${sectionLabel(section)} $year", back = { showYearList(section) }) { col ->
            col.addView(bigButton("テスト形式で解く", "${list.size}問を順番に・自動採点", cAccent) {
                startSequential(section, year, list, 0, ArrayList())
            })
            col.addView(bigButton("この年度からランダム", "1問ずつランダム出題", 0xFF15A6A0.toInt()) {
                startRandomPool(list, section)
            })
        })
    }

    private fun showPmList() {
        setContent(screen("午後 (記述式)", back = { showStudyHome() }) { col ->
            col.addView(tv("公式の問題冊子・解答例(PDF)を開きます。記述式は自己採点し、AIタブの自己添削も活用してください。", 13f, cSub))
            col.addView(spacer(12))
            for (pm in QuestionData.pmExams) {
                val c = card()
                c.addView(tv(pm.label, 15f, cText, true))
                c.addView(spacer(8))
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                val b1 = smallBtn("問題PDF", cAccent) { openUrl(pm.questionsPdf) }
                val b2 = smallBtn("解答例PDF", cCard2) { openUrl(pm.answersPdf) }
                row.addView(b1)
                row.addView(spacer2(10))
                row.addView(b2)
                c.addView(row)
                col.addView(c)
            }
        })
    }

    private fun smallBtn(label: String, bg: Int, onClick: () -> Unit): View {
        val b = tv(label, 13.5f, Color.WHITE, true)
        b.gravity = Gravity.CENTER
        b.background = rounded(bg, 10)
        b.setPadding(dp(14), dp(10), dp(14), dp(10))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun spacer2(w: Int): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(dp(w), 1)
        return v
    }

    // ============================================================
    // 問題演習エンジン
    // ============================================================

    /** 単発ランダム(n問で終了) */
    private fun startRandom(n: Int) {
        val pool = (QuestionData.am2ByYear.values.flatten() + QuestionData.am1ByYear.values.flatten())
        runRandomSession(pool, n, 0, ArrayList())
    }

    private fun runRandomSession(pool: List<QuestionData.Question>, total: Int, idx: Int, results: ArrayList<Boolean>) {
        if (idx >= total) { showSessionResult(results, "クイック学習") { showTab(0) }; return }
        val q = pool.random()
        val section = sectionOf(q)
        showQuestion(section, q, "${idx + 1} / $total", { showTab(0) }) { correct ->
            results.add(correct)
            runRandomSession(pool, total, idx + 1, results)
        }
    }

    /** 無限ランダム(戻るまで) */
    private fun startRandomPool(pool: List<QuestionData.Question>, section: String) {
        if (pool.isEmpty()) return
        val q = pool.random()
        showQuestion(section, q, "ランダム", { showTab(currentTab) }) {
            startRandomPool(pool, section)
        }
    }

    /** テスト形式(年度を順番に、最後に採点) */
    private fun startSequential(section: String, year: String, list: List<QuestionData.Question>, idx: Int, results: ArrayList<Boolean>) {
        if (idx >= list.size) {
            showSessionResult(results, "$year ${sectionLabel(section)}") { showYearMenu(section, year, list) }
            return
        }
        showQuestion(section, list[idx], "${idx + 1} / ${list.size}", { showYearList(section) }) { correct ->
            results.add(correct)
            startSequential(section, year, list, idx + 1, results)
        }
    }

    private fun sectionOf(q: QuestionData.Question): String =
        if (QuestionData.am1ByYear.values.any { l -> l.any { it === q } }) "am1" else "am2"

    private fun fmtMs(ms: Long): String {
        val s = (ms / 1000).toInt().coerceAtLeast(0)
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    /**
     * 問題画面。選択肢タップで自動採点・色分け・解説表示、記録して onDone(correct)。
     * 模試ではtimerStart/timerLimitMsでタイマー、onFlagToggleで見直しフラグを表示。
     */
    private fun showQuestion(
        section: String,
        q: QuestionData.Question,
        progress: String,
        onBack: () -> Unit,
        timerStart: Long? = null,
        timerLimitMs: Long? = null,
        flagged: Boolean = false,
        onFlagToggle: (() -> Unit)? = null,
        allowHint: Boolean = true,
        onDone: (Boolean) -> Unit
    ) {
        val choices = q.choices
        setContent(screen("${sectionLabel(section)}  問${q.no}", back = onBack) { col ->
            val meta = LinearLayout(this)
            meta.orientation = LinearLayout.HORIZONTAL
            meta.gravity = Gravity.CENTER_VERTICAL
            meta.addView(pill(q.year, cCard2))
            meta.addView(spacer2(8))
            val prog = tv(progress, 12f, cSub)
            prog.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            meta.addView(prog)
            if (timerStart != null && timerLimitMs != null) {
                val timerView = tv("", 13f, cSub, true)
                meta.addView(timerView)
                val tick = object : Runnable {
                    override fun run() {
                        val remain = timerLimitMs - (System.currentTimeMillis() - timerStart)
                        timerView.text = if (remain >= 0) "残り ${fmtMs(remain)}" else "超過 ${fmtMs(-remain)}"
                        timerView.setTextColor(if (remain >= 0) cSub else cRed)
                        handler.postDelayed(this, 1000)
                    }
                }
                tick.run()
            }
            col.addView(meta)

            if (onFlagToggle != null) {
                col.addView(spacer(8))
                var isFlagged = flagged
                val flag = tv(if (isFlagged) "★ 見直しに登録済み" else "☆ 見直しに登録", 13f, if (isFlagged) cGold else cSub, true)
                flag.background = rounded(cCard2, 10)
                flag.setPadding(dp(12), dp(8), dp(12), dp(8))
                flag.setOnClickListener {
                    onFlagToggle()
                    isFlagged = !isFlagged
                    flag.text = if (isFlagged) "★ 見直しに登録済み" else "☆ 見直しに登録"
                    flag.setTextColor(if (isFlagged) cGold else cSub)
                }
                col.addView(flag)
            }
            col.addView(spacer(12))

            val qc = card()
            qc.addView(tv(q.text, 15.5f, cText))
            col.addView(qc)

            if (choices == null) { onDone(true); return@screen }

            // ヒント(答えは伏せる)。設定で常時表示、それ以外はボタンで表示。
            if (allowHint) {
                val hintHolder = LinearLayout(this)
                hintHolder.orientation = LinearLayout.VERTICAL
                fun revealHint() {
                    if (hintHolder.childCount > 0) return
                    val hc = card()
                    hc.background = rounded(0xFF2A2510.toInt())
                    hc.addView(tv("ヒント", 13f, cGold, true))
                    hc.addView(spacer(4))
                    hc.addView(tv(Hint.of(q), 13.5f, 0xFFEADFB8.toInt()))
                    hintHolder.addView(hc)
                }
                if (Store.hintAlways()) {
                    revealHint()
                } else {
                    val hb = tv("ヒントを見る", 13f, cGold, true)
                    hb.background = rounded(cCard2, 10)
                    hb.setPadding(dp(14), dp(10), dp(14), dp(10))
                    val hbLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    hbLp.bottomMargin = dp(10)
                    hb.layoutParams = hbLp
                    hb.setOnClickListener { revealHint(); hb.visibility = View.GONE }
                    col.addView(hb)
                }
                col.addView(hintHolder)
            }

            val labels = listOf("ア", "イ", "ウ", "エ")
            val buttons = ArrayList<TextView>()
            var answered = false
            val explanationHolder = LinearLayout(this)
            explanationHolder.orientation = LinearLayout.VERTICAL

            for (i in choices.indices) {
                val btn = tv("${labels[i]}  ${choices[i]}", 14.5f, cText)
                btn.background = rounded(cCard2)
                btn.setPadding(dp(14), dp(13), dp(14), dp(13))
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(10)
                btn.layoutParams = lp
                btn.setOnClickListener {
                    if (answered) return@setOnClickListener
                    answered = true
                    val correct = i == q.answerIndex
                    for (j in buttons.indices) {
                        when (j) {
                            q.answerIndex -> buttons[j].background = rounded(cGreen)
                            i -> if (!correct) buttons[j].background = rounded(cRed)
                        }
                        if (j == q.answerIndex || (j == i && !correct))
                            buttons[j].setTextColor(Color.WHITE)
                    }
                    Store.recordAnswer(section, q.year, q.no, correct)
                    // 解説
                    val ec = card()
                    ec.background = rounded(if (correct) 0xFF14331F.toInt() else 0xFF3A1B1D.toInt())
                    ec.addView(tv(if (correct) "正解！" else "不正解", 15f, if (correct) cGreen else cRed, true))
                    ec.addView(spacer(6))
                    ec.addView(tv("正解: ${q.answerText}", 14f, cText, true))
                    ec.addView(spacer(6))
                    ec.addView(tv(q.explanation, 13.5f, 0xFFD7E0EA.toInt()))
                    explanationHolder.addView(ec)
                    val next = bigButton("次へ", null, cAccent) { onDone(correct) }
                    explanationHolder.addView(next)
                }
                buttons.add(btn)
                col.addView(btn)
            }
            col.addView(explanationHolder)
        })
    }

    private fun showSessionResult(results: List<Boolean>, title: String, onClose: () -> Unit) {
        val correct = results.count { it }
        val total = results.size
        val rate = if (total == 0) 0 else correct * 100 / total
        setContent(screen("結果") { col ->
            val c = card()
            c.addView(tv(title, 14f, cSub))
            c.addView(spacer(6))
            c.addView(tv("$correct / $total 問正解", 26f, cText, true))
            c.addView(spacer(6))
            c.addView(tv("正答率 $rate%", 16f, if (rate >= 60) cGreen else cGold, true))
            col.addView(c)
            if (total > 0 && rate < 60)
                col.addView(tv("合格ラインの目安は60%。間違えた問題は復習キューに入りました。", 13f, cSub))
            col.addView(spacer(12))
            col.addView(bigButton("復習する", "間違えた問題をもう一度", 0xFF7A5AF8.toInt()) { showReview() })
            col.addView(listButton("とじる") { onClose() })
        })
    }

    // ============================================================
    // 復習(忘却曲線 + 間違えた問題)
    // ============================================================
    private fun resolveId(id: String): Pair<String, QuestionData.Question>? {
        val parts = id.split("|")
        if (parts.size != 3) return null
        val (sec, year, noStr) = parts
        val no = noStr.toIntOrNull() ?: return null
        val list = sectionMap(sec)[year] ?: return null
        val q = list.firstOrNull { it.no == no } ?: return null
        return sec to q
    }

    private fun showReview() {
        val ids = (Store.dueIds() + Store.wrongIds()).distinct()
        val queue = ids.mapNotNull { resolveId(it) }
        if (queue.isEmpty()) {
            setContent(screen("復習") { col ->
                val c = card()
                c.addView(tv("いま復習する問題はありません", 15f, cText, true))
                c.addView(spacer(6))
                c.addView(tv("問題を解いて間違えると、ここに復習キューとして溜まります。忘却曲線に沿って最適なタイミングで再出題します。", 13f, cSub))
                col.addView(c)
                col.addView(bigButton("クイック学習をする", null, cAccent) { startRandom(10) })
            })
            return
        }
        runReview(queue, 0, ArrayList())
    }

    private fun runReview(queue: List<Pair<String, QuestionData.Question>>, idx: Int, results: ArrayList<Boolean>) {
        if (idx >= queue.size) { showSessionResult(results, "復習") { showTab(currentTab) }; return }
        val (section, q) = queue[idx]
        showQuestion(section, q, "復習 ${idx + 1} / ${queue.size}", { showTab(currentTab) }) { correct ->
            results.add(correct)
            runReview(queue, idx + 1, results)
        }
    }

    // ============================================================
    // 模試(時間計測・見直しフラグ)
    // ============================================================
    private fun showMockPick() {
        setContent(screen("模試") { col ->
            col.addView(tv("本番同様に時間を計って通しで解きます。午前Ⅱ=40分・午前Ⅰ=50分。見直したい問題には★を付けられます。", 13f, cSub))
            col.addView(spacer(12))
            col.addView(sectionHeader("午前Ⅱ (セキュリティ・25問・40分)"))
            for ((year, list) in QuestionData.am2ByYear)
                col.addView(listButton("$year", "${list.size}問") { startMock("am2", year, list, 40 * 60_000L) })
            col.addView(spacer(6))
            col.addView(sectionHeader("午前Ⅰ (共通・30問・50分)"))
            for ((year, list) in QuestionData.am1ByYear)
                col.addView(listButton("$year", "${list.size}問") { startMock("am1", year, list, 50 * 60_000L) })
        })
    }

    private fun startMock(section: String, year: String, list: List<QuestionData.Question>, limitMs: Long) {
        val start = System.currentTimeMillis()
        val results = ArrayList<Boolean>()
        val flagged = HashSet<Int>()
        runMock(section, year, list, 0, results, flagged, start, limitMs)
    }

    private fun runMock(
        section: String, year: String, list: List<QuestionData.Question>,
        idx: Int, results: ArrayList<Boolean>, flagged: HashSet<Int>,
        start: Long, limitMs: Long
    ) {
        if (idx >= list.size) {
            showMockResult(section, year, list, results, flagged, System.currentTimeMillis() - start)
            return
        }
        val no = list[idx].no
        showQuestion(
            section, list[idx], "${idx + 1} / ${list.size}",
            onBack = { showMockPick() },
            timerStart = start, timerLimitMs = limitMs,
            flagged = flagged.contains(no),
            onFlagToggle = { if (flagged.contains(no)) flagged.remove(no) else flagged.add(no) },
            allowHint = false
        ) { correct ->
            results.add(correct)
            runMock(section, year, list, idx + 1, results, flagged, start, limitMs)
        }
    }

    private fun showMockResult(
        section: String, year: String, list: List<QuestionData.Question>,
        results: List<Boolean>, flagged: HashSet<Int>, elapsedMs: Long
    ) {
        val correct = results.count { it }
        val total = results.size
        val rate = if (total == 0) 0 else correct * 100 / total
        setContent(screen("模試 結果") { col ->
            val c = card()
            c.addView(tv("$year ${sectionLabel(section)}", 14f, cSub))
            c.addView(spacer(6))
            c.addView(tv("$correct / $total 問正解", 26f, cText, true))
            c.addView(spacer(6))
            c.addView(tv("正答率 $rate%", 17f, if (rate >= 60) cGreen else cGold, true))
            c.addView(spacer(4))
            c.addView(tv("解答時間 ${fmtMs(elapsedMs)}", 14f, cSub))
            col.addView(c)

            // 分野別スコア(この年度内)
            val byCat = LinkedHashMap<String, IntArray>() // cat -> [att, cor]
            for (i in list.indices) {
                val cat = Category.categoryOf(list[i])
                val arr = byCat.getOrPut(cat) { intArrayOf(0, 0) }
                arr[0]++
                if (i < results.size && results[i]) arr[1]++
            }
            val sc = card()
            sc.addView(tv("分野別スコア", 14f, cText, true))
            sc.addView(spacer(10))
            for ((cat, arr) in byCat) {
                val r = if (arr[0] == 0) 0 else arr[1] * 100 / arr[0]
                sc.addView(rateRow("$cat (${arr[1]}/${arr[0]})", arr[0], r, cAccent))
            }
            col.addView(sc)

            if (flagged.isNotEmpty()) {
                val fc = card()
                fc.addView(tv("見直し登録した問題", 14f, cGold, true))
                fc.addView(spacer(6))
                fc.addView(tv("問 " + flagged.sorted().joinToString(", "), 14f, cText))
                fc.addView(spacer(8))
                fc.addView(tv("復習キューにも間違えた問題が入っています。", 12f, cSub))
                col.addView(fc)
            }

            col.addView(bigButton("間違えた問題を復習", null, 0xFF7A5AF8.toInt()) { showReview() })
            col.addView(listButton("模試メニューへ") { showMockPick() })
        })
    }

    // ============================================================
    // 知識マップ
    // ============================================================
    private fun showKnowledgeMap() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(cBg)

        // ヘッダ
        val head = LinearLayout(this)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(dp(16), dp(14), dp(16), dp(8))
        val bk = tv("‹ もどる", 14f, cAccent, true)
        bk.setOnClickListener { showStudyHome() }
        head.addView(bk)
        head.addView(spacer(6))
        head.addView(tv("知識マップ", 20f, cText, true))
        head.addView(tv("中心=試験全体 / 円=分野(数字は推奨学習順) / 金の矢印=前提→発展。ピンチで拡大、ドラッグで移動、分野をタップで詳細。", 11.5f, cSub))
        root.addView(head)

        // マップ本体
        val map = MapView(this) { cat -> showMapCategory(cat) }
        map.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(map)

        // フッタ操作
        val foot = LinearLayout(this)
        foot.orientation = LinearLayout.HORIZONTAL
        foot.setPadding(dp(16), dp(8), dp(16), dp(12))
        val zoomOut = smallBtn("－", cCard2) { map.zoom(0.8f) }
        zoomOut.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f)
        val zoomIn = smallBtn("＋", cCard2) { map.zoom(1.25f) }
        zoomIn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f)
        val reset = smallBtn("リセット", cCard2) { map.resetView() }
        reset.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f)
        val orderBtn = smallBtn("学習順で見る", cAccent) { showLearningOrder() }
        orderBtn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
        foot.addView(zoomOut); foot.addView(spacer2(8))
        foot.addView(zoomIn); foot.addView(spacer2(8))
        foot.addView(reset); foot.addView(spacer2(8))
        foot.addView(orderBtn)
        root.addView(foot)

        setContent(root)
    }

    /** 学習順のリスト表示(マップの補助。1画面で順序を把握) */
    private fun showLearningOrder() {
        setContent(screen("学習順ガイド", back = { showKnowledgeMap() }) { col ->
            col.addView(tv("前提→発展の順に並べた推奨ルートです。上から進めると土台が崩れません。", 13f, cSub))
            col.addView(spacer(12))
            for (b in KnowledgeMap.inLearningOrder()) {
                val c = card()
                val head = LinearLayout(this)
                head.orientation = LinearLayout.HORIZONTAL
                head.gravity = Gravity.CENTER_VERTICAL
                head.addView(pill("${b.order}", cAccent))
                head.addView(spacer2(10))
                val t = tv(b.category, 16f, cText, true)
                t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                head.addView(t)
                c.addView(head)
                c.addView(spacer(8))
                c.addView(tv(b.summary, 13f, cSub))
                c.addView(spacer(10))
                c.addView(listButton("詳しく見る") { showMapCategory(b.category) })
                col.addView(c)
            }
        })
    }

    /** マップの分野をタップしたときの詳細。中分類・小分類＋演習/用語辞典への動線。 */
    private fun showMapCategory(cat: String) {
        val b = KnowledgeMap.byCategory(cat)
        setContent(screen(cat, back = { showKnowledgeMap() }) { col ->
            if (b == null) {
                col.addView(tv("この分野の詳細データがありません。", 13f, cSub))
                return@screen
            }
            // 概要 + 自分の到達度
            val c = card()
            val head = LinearLayout(this)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            head.addView(pill("学習順 ${b.order}", cAccent))
            head.addView(spacer2(8))
            val stats = categoryStats()[cat]
            if (stats != null && stats[0] > 0) {
                val r = stats[1] * 100 / stats[0]
                head.addView(pill("正答率 ${r}%", if (r < 50) cRed else if (r < 70) cGold else cGreen))
            } else {
                head.addView(pill("未挑戦", cCard2))
            }
            c.addView(head)
            c.addView(spacer(10))
            c.addView(tv(b.summary, 13.5f, cText))
            col.addView(c)

            // 前提・発展
            val pre = KnowledgeMap.prereq.filter { it.second == cat }.map { it.first }
            val next = KnowledgeMap.prereq.filter { it.first == cat }.map { it.second }
            if (pre.isNotEmpty() || next.isNotEmpty()) {
                val rc = card()
                rc.addView(tv("知識のつながり", 14f, cGold, true))
                rc.addView(spacer(8))
                if (pre.isNotEmpty()) {
                    rc.addView(tv("前提となる分野", 12f, cSub))
                    rc.addView(spacer(6))
                    for (p in pre) rc.addView(listButton("← $p") { showMapCategory(p) })
                }
                if (next.isNotEmpty()) {
                    rc.addView(spacer(6))
                    rc.addView(tv("ここから発展する分野", 12f, cSub))
                    rc.addView(spacer(6))
                    for (nx in next) rc.addView(listButton("→ $nx") { showMapCategory(nx) })
                }
                col.addView(rc)
            }

            // 中分類・小分類・関連用語
            for (m in b.mid) {
                val mc = card()
                val mh = LinearLayout(this)
                mh.orientation = LinearLayout.HORIZONTAL
                mh.gravity = Gravity.CENTER_VERTICAL
                val mt = tv(m.name, 15f, cAccent, true)
                mt.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                mh.addView(mt)
                if (m.terms.isNotEmpty()) mh.addView(pill("${m.terms.size}語", cCard2))
                mc.addView(mh)
                mc.addView(spacer(8))
                for (s in m.small) {
                    val row = tv("・$s", 13.5f, cText)
                    row.setPadding(0, dp(3), 0, dp(3))
                    mc.addView(row)
                }
                if (m.terms.isNotEmpty()) {
                    mc.addView(spacer(10))
                    mc.addView(tv("関連用語（タップで意味を表示）", 11.5f, cSub))
                    mc.addView(spacer(6))
                    mc.addView(termChips(m.terms))
                }
                col.addView(mc)
            }

            // 学習アクション
            val ac = card()
            ac.addView(tv("この分野を学ぶ", 14f, cGold, true))
            ac.addView(spacer(10))
            val n = Category.counts()[cat] ?: 0
            if (n > 0) ac.addView(listButton("問題を解く", "${n}問から集中演習") { startCategoryDrill(cat) })
            ac.addView(listButton("用語辞典で見る", "関連する用語をまとめて確認") {
                showGlossary("", if (Glossary.categories.contains(b.glossaryCat)) b.glossaryCat else "すべて")
            })
            col.addView(ac)
        })
    }

    /** 用語チップを折り返して並べる。タップでその用語の意味を表示。 */
    private fun termChips(terms: List<String>): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        var row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        var used = 0
        val perRow = 3
        for (t in terms) {
            if (used >= perRow) {
                wrap.addView(row)
                wrap.addView(spacer(6))
                row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                used = 0
            }
            val chip = tv(t, 12f, cText, true)
            chip.background = rounded(cCard2, 16)
            chip.setPadding(dp(10), dp(6), dp(10), dp(6))
            chip.gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginEnd = dp(6)
            chip.layoutParams = lp
            chip.setOnClickListener { showTermDialog(t) }
            row.addView(chip)
            used++
        }
        if (used > 0) {
            while (used < perRow) {
                val filler = View(this)
                filler.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                row.addView(filler)
                used++
            }
            wrap.addView(row)
        }
        return wrap
    }

    private fun showTermDialog(term: String) {
        val t = Glossary.terms.firstOrNull { it.term == term }
        if (t == null) {
            showGlossary(term, "すべて")
            return
        }
        val msg = StringBuilder()
        if (t.full.isNotEmpty()) msg.append(t.full).append("\n\n")
        msg.append(t.desc)
        if (t.etym.isNotEmpty()) msg.append("\n\n語源: ").append(t.etym)
        AlertDialog.Builder(this)
            .setTitle(t.term)
            .setMessage(msg.toString())
            .setPositiveButton("閉じる", null)
            .setNeutralButton("辞典で見る") { _, _ -> showGlossary(t.term, "すべて") }
            .show()
    }

    // ============================================================
    // 単語帳(フラッシュカード)
    // ============================================================
    private fun showFlashcards() {
        val pool = (QuestionData.am2ByYear.values.flatten() + QuestionData.am1ByYear.values.flatten())
        // 復習期限が来ているものを優先、なければランダム
        val due = Store.dueIds().mapNotNull { resolveId(it) }
        val q = if (due.isNotEmpty()) due.random().second else pool.random()
        val section = sectionOf(q)
        flashCard(section, q)
    }

    private fun flashCard(section: String, q: QuestionData.Question) {
        setContent(screen("単語帳", back = { showStudyHome() }) { col ->
            val c = card()
            c.addView(pill("${sectionLabel(section)} ${q.year} 問${q.no}", cCard2))
            c.addView(spacer(10))
            c.addView(tv(q.text, 15.5f, cText))
            col.addView(c)

            val answerHolder = LinearLayout(this)
            answerHolder.orientation = LinearLayout.VERTICAL
            col.addView(answerHolder)

            col.addView(bigButton("答えを見る", null, cAccent) {
                if (answerHolder.childCount > 0) return@bigButton
                val ac = card()
                ac.background = rounded(cCard2)
                ac.addView(tv("正解: ${q.answerText}", 15f, cGreen, true))
                if (q.choices != null) {
                    ac.addView(spacer(4))
                    ac.addView(tv(q.choices[q.answerIndex], 14f, cText))
                }
                ac.addView(spacer(8))
                ac.addView(tv(q.explanation, 13.5f, 0xFFD7E0EA.toInt()))
                answerHolder.addView(ac)

                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                val ok = smallBtn("覚えた", cGreen) {
                    Store.recordFlash(Store.qid(section, q.year, q.no), true); showFlashcards()
                }
                ok.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                val ng = smallBtn("まだ", cRed) {
                    Store.recordFlash(Store.qid(section, q.year, q.no), false); showFlashcards()
                }
                ng.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(ok); row.addView(spacer2(10)); row.addView(ng)
                answerHolder.addView(row)
            })
            col.addView(listButton("次のカード（答えを見ずに）") { showFlashcards() })
        })
    }

    // ============================================================
    // 用語辞典
    // ============================================================
    private fun showGlossary(filter: String = "", cat: String = "すべて") {
        setContent(screen("用語辞典", back = { showStudyHome() }) { col ->
            val input = EditText(this)
            input.hint = "用語を検索(例: DKIM、ゼロトラスト)"
            input.setText(filter)
            input.setTextColor(cText)
            input.setHintTextColor(cSub)
            input.background = rounded(cCard2, 10)
            input.setPadding(dp(14), dp(12), dp(14), dp(12))
            col.addView(input)
            col.addView(spacer(6))
            val search = smallBtn("検索", cAccent) { showGlossary(input.text.toString(), cat) }
            col.addView(search)
            col.addView(spacer(10))

            // 分野フィルタ(横スクロールのチップ)
            val hs = HorizontalScrollView(this)
            hs.isHorizontalScrollBarEnabled = false
            val chips = LinearLayout(this)
            chips.orientation = LinearLayout.HORIZONTAL
            val allCats = listOf("すべて") + Glossary.categories
            for (cc in allCats) {
                val sel = cc == cat
                val chip = tv(cc, 12.5f, if (sel) Color.WHITE else cSub, true)
                chip.background = rounded(if (sel) cAccent else cCard2, 20)
                chip.setPadding(dp(14), dp(7), dp(14), dp(7))
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = dp(8)
                chip.layoutParams = lp
                chip.setOnClickListener { showGlossary(filter, cc) }
                chips.addView(chip)
            }
            hs.addView(chips)
            col.addView(hs)
            col.addView(spacer(12))

            val f = filter.trim()
            val items = Glossary.terms.filter { t ->
                (cat == "すべて" || t.cat == cat) &&
                (f.isEmpty() || t.term.contains(f, true) || t.full.contains(f, true) ||
                 t.desc.contains(f) || t.etym.contains(f, true))
            }

            col.addView(tv("${items.size}語", 12f, cSub))
            col.addView(spacer(8))
            if (items.isNotEmpty()) {
                val cardRow = LinearLayout(this)
                cardRow.orientation = LinearLayout.HORIZONTAL
                val b1 = smallBtn("暗記: 単語→意味", 0xFF7A5AF8.toInt()) { showGlossaryCard(filter, cat, "t2m") }
                b1.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                val b2 = smallBtn("暗記: 意味→単語", 0xFF15A6A0.toInt()) { showGlossaryCard(filter, cat, "m2t") }
                b2.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                cardRow.addView(b1); cardRow.addView(spacer2(10)); cardRow.addView(b2)
                col.addView(cardRow)
                col.addView(spacer(10))
            }

            for (t in items) {
                val c = card()
                c.addView(tv(t.term, 15.5f, cAccent, true))
                if (t.full.isNotEmpty()) c.addView(tv(t.full, 11.5f, cSub))
                c.addView(spacer(6))
                c.addView(tv(t.desc, 13.5f, cText))
                if (t.etym.isNotEmpty()) {
                    c.addView(spacer(6))
                    c.addView(tv("語源: ${t.etym}", 11.5f, cGold))
                }
                col.addView(c)
            }
            if (items.isEmpty())
                col.addView(tv("該当する用語がありません。", 13f, cSub))
        })
    }

    /**
     * 用語カード。mode="t2m": 表=単語→裏=意味。mode="m2t": 表=意味→裏=単語。
     */
    private fun showGlossaryCard(filter: String, cat: String, mode: String) {
        val f = filter.trim()
        val pool = Glossary.terms.filter { t ->
            (cat == "すべて" || t.cat == cat) &&
            (f.isEmpty() || t.term.contains(f, true) || t.full.contains(f, true) ||
             t.desc.contains(f) || t.etym.contains(f, true))
        }
        if (pool.isEmpty()) { showGlossary(filter, cat); return }
        val t = pool.random()
        val m2t = mode == "m2t"
        val title = if (m2t) "用語カード（意味→単語）" else "用語カード（単語→意味）"
        setContent(screen(title, back = { showGlossary(filter, cat) }) { col ->
            col.addView(pill(t.cat, cCard2))
            col.addView(spacer(16))

            // 表(front)
            val c = card()
            c.setPadding(dp(20), dp(24), dp(20), dp(24))
            if (m2t) {
                c.addView(tv("この意味の用語は？", 12f, cSub).apply { gravity = Gravity.CENTER })
                c.addView(spacer(10))
                c.addView(tv(t.desc, 16.5f, cText, true).apply { gravity = Gravity.CENTER })
            } else {
                c.addView(tv(t.term, 24f, cText, true).apply { gravity = Gravity.CENTER })
                if (t.full.isNotEmpty()) {
                    val fl = tv(t.full, 12.5f, cSub)
                    fl.gravity = Gravity.CENTER
                    fl.setPadding(0, dp(8), 0, 0)
                    c.addView(fl)
                }
            }
            col.addView(c)

            // 裏(back) 表示領域
            val back = LinearLayout(this)
            back.orientation = LinearLayout.VERTICAL
            col.addView(back)

            val revealLabel = if (m2t) "単語を見る" else "意味を見る"
            col.addView(bigButton(revealLabel, null, cAccent) {
                if (back.childCount > 0) return@bigButton
                val ac = card()
                ac.background = rounded(cCard2)
                if (m2t) {
                    ac.addView(tv(t.term, 22f, cAccent, true))
                    if (t.full.isNotEmpty()) {
                        ac.addView(spacer(4))
                        ac.addView(tv(t.full, 12.5f, cSub))
                    }
                } else {
                    ac.addView(tv(t.desc, 14.5f, cText))
                }
                if (t.etym.isNotEmpty()) {
                    ac.addView(spacer(8))
                    ac.addView(tv("語源: ${t.etym}", 12.5f, cGold))
                }
                back.addView(ac)
                back.addView(bigButton("次のカード", null, 0xFF7A5AF8.toInt()) { showGlossaryCard(filter, cat, mode) })
            })
            col.addView(listButton("次のカード（答えを見ずに）") { showGlossaryCard(filter, cat, mode) })
        })
    }

    // ============================================================
    // タブ2: AI
    // ============================================================
    private fun showAiHome() {
        setContent(screen("AI先生") { col ->
            // 解説をさがす
            val c = card()
            c.addView(tv("わからない用語・問題を調べる", 15f, cText, true))
            c.addView(spacer(8))
            val input = EditText(this)
            input.hint = "キーワードを入力(例: DNSSEC 仕組み)"
            input.setTextColor(cText)
            input.setHintTextColor(cSub)
            input.background = rounded(cCard2, 10)
            input.setPadding(dp(14), dp(12), dp(14), dp(12))
            c.addView(input)
            c.addView(spacer(8))
            c.addView(smallBtn("解説動画をさがす (YouTube)", cAccent) {
                val kw = input.text.toString().trim()
                if (kw.isNotEmpty())
                    openUrl("https://www.youtube.com/results?search_query=" + Uri.encode("情報処理安全確保支援士 $kw 解説"))
            })
            col.addView(c)

            // 午後 自己採点
            val c2 = card()
            c2.addView(tv("午後(記述)の自己採点", 15f, cText, true))
            c2.addView(spacer(6))
            c2.addView(tv("公式の問題PDFで解答し、解答例PDFと照合。設問ごとに○△×で自己採点して達成度を出します。", 13f, cSub))
            c2.addView(spacer(10))
            c2.addView(smallBtn("自己採点シートを開く", cAccent) { showPmScoring() })
            col.addView(c2)

            // 端末内AI連携(準備中)
            val c3 = card()
            c3.addView(tv("端末内AIチャット", 15f, cText, true))
            c3.addView(pill("準備中", cGold))
            c3.addView(spacer(8))
            c3.addView(tv("オフラインで動く端末内AI(Bonsai)との連携を予定しています。現時点では上の解説検索をご利用ください。", 13f, cSub))
            col.addView(c3)

            // OCR / 音声(準備中)
            val c4 = card()
            c4.addView(tv("OCR学習 / 音声学習", 15f, cText, true))
            c4.addView(pill("準備中", cGold))
            c4.addView(spacer(8))
            c4.addView(tv("紙の問題の読み取りや、耳で聞く学習は今後のバージョンで対応します。", 13f, cSub))
            col.addView(c4)
        })
    }

    // ============================================================
    // 午後 自己採点シート
    // ============================================================
    private fun showPmScoring() {
        setContent(screen("午後 自己採点", back = { showTab(2) }) { col ->
            col.addView(tv("採点する回を選んでください。", 13f, cSub))
            col.addView(spacer(12))
            for (pm in QuestionData.pmExams)
                col.addView(listButton(pm.label) { showPmSheet(pm, 10) })
        })
    }

    private fun showPmSheet(pm: QuestionData.PmExam, rows: Int) {
        val scores = IntArray(rows) { -1 } // 2=○,1=△,0=×,-1=未
        setContent(screen(pm.label, back = { showPmScoring() }) { col ->
            // PDFリンク
            val linkRow = LinearLayout(this)
            linkRow.orientation = LinearLayout.HORIZONTAL
            val b1 = smallBtn("問題PDF", cAccent) { openUrl(pm.questionsPdf) }
            val b2 = smallBtn("解答例PDF", cCard2) { openUrl(pm.answersPdf) }
            linkRow.addView(b1); linkRow.addView(spacer2(10)); linkRow.addView(b2)
            col.addView(linkRow)
            col.addView(spacer(10))
            col.addView(tv("設問数を選択:", 12.5f, cSub))
            col.addView(spacer(6))
            val nRow = LinearLayout(this)
            nRow.orientation = LinearLayout.HORIZONTAL
            for (n in listOf(5, 8, 10, 12, 15)) {
                val b = smallBtn("$n", if (n == rows) cAccent else cCard2) { showPmSheet(pm, n) }
                b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = dp(6) }
                nRow.addView(b)
            }
            col.addView(nRow)
            col.addView(spacer(14))

            // 集計表示
            val tally = tv("", 14f, cText, true)
            fun refreshTally() {
                var o = 0; var t = 0; var x = 0; var un = 0
                var got = 0.0; var ans = 0
                for (s in scores) when (s) {
                    2 -> { o++; got += 1.0; ans++ }
                    1 -> { t++; got += 0.5; ans++ }
                    0 -> { x++; ans++ }
                    else -> un++
                }
                val pct = if (ans == 0) 0 else (got / ans * 100).toInt()
                tally.text = "○ $o  △ $t  × $x  未 $un   達成度 $pct%"
                tally.setTextColor(if (ans == 0) cSub else if (pct >= 60) cGreen else cGold)
            }

            // 設問ごとの○△×
            for (i in 0 until rows) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8)
                row.layoutParams = lp
                val label = tv("設問 ${i + 1}", 14f, cText)
                label.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(label)

                val marks = arrayOfNulls<TextView>(3) // ×,△,○ = index0,1,2 に対応(値0,1,2)
                fun paint() {
                    for (v in 0..2) {
                        val sel = scores[i] == v
                        val selColor = if (v == 2) cGreen else if (v == 1) cGold else cRed
                        marks[v]!!.background = rounded(if (sel) selColor else cCard2, 8)
                        marks[v]!!.setTextColor(if (sel) Color.WHITE else cSub)
                    }
                }
                val symbols = listOf("×" to 0, "△" to 1, "○" to 2)
                for ((sym, v) in symbols) {
                    val m = tv(sym, 16f, cSub, true)
                    m.gravity = Gravity.CENTER
                    m.setPadding(dp(14), dp(6), dp(14), dp(6))
                    m.setOnClickListener { scores[i] = v; paint(); refreshTally() }
                    marks[v] = m
                    row.addView(m)
                    row.addView(spacer2(6))
                }
                paint()
                col.addView(row)
            }

            col.addView(spacer(8))
            val tc = card()
            tc.background = rounded(cCard2)
            tc.addView(tally)
            col.addView(tc)
            refreshTally()

            col.addView(spacer(6))
            col.addView(tv("※ ○=要点を満たす / △=部分的 / ×=不十分。配点は回により異なるため、達成度は自己評価の目安です。正解の根拠は必ず公式の解答例PDFで確認してください。", 12f, cSub))
        })
    }
    private fun bar(percent: Int, fill: Int): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.HORIZONTAL
        wrap.background = rounded(cCard2, 8)
        val h = dp(12)
        wrap.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
        val done = View(this)
        done.background = rounded(fill, 8)
        done.layoutParams = LinearLayout.LayoutParams(0, h, percent.coerceIn(0, 100).toFloat())
        val rest = View(this)
        rest.layoutParams = LinearLayout.LayoutParams(0, h, (100 - percent.coerceIn(0, 100)).toFloat())
        wrap.addView(done); wrap.addView(rest)
        return wrap
    }

    private fun rateRow(label: String, att: Int, rate: Int, fill: Int): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        col.layoutParams = lp
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        val l = tv(label, 13.5f, cText, true)
        l.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        head.addView(l)
        head.addView(tv(if (att == 0) "未挑戦" else "$rate% ($att)", 13f, if (att == 0) cSub else cText))
        col.addView(head)
        col.addView(spacer(6))
        col.addView(bar(rate, fill))
        return col
    }

    private fun showAnalysis() {
        setContent(screen("学習分析") { col ->
            if (Store.totalAnswered() == 0) {
                val c = card()
                c.addView(tv("まだデータがありません", 15f, cText, true))
                c.addView(spacer(6))
                c.addView(tv("問題を解くと、正答率や苦手分野、合格可能性の目安をここに表示します。", 13f, cSub))
                col.addView(c)
                return@screen
            }

            // 合格可能性
            val est = Store.passEstimate()
            val pc = card()
            pc.addView(tv("合格可能性の目安", 14f, cGold, true))
            pc.addView(spacer(6))
            if (est < 0) {
                pc.addView(tv("推定にはもう少しデータが必要です(10問以上)。", 13f, cSub))
            } else {
                val color = if (est >= 80) cGreen else if (est >= 50) cGold else cRed
                pc.addView(tv("$est%", 30f, color, true))
                pc.addView(spacer(4))
                pc.addView(bar(est, color))
                pc.addView(spacer(6))
                pc.addView(tv("午前Ⅱを主要因に、合格ライン60%への到達度を簡易推定した目安です。", 12f, cSub))
            }
            col.addView(pc)

            // セクション別
            val sc = card()
            sc.addView(tv("セクション別 正答率", 14f, cText, true))
            sc.addView(spacer(10))
            sc.addView(rateRow("午前Ⅱ (セキュリティ)", Store.sectionAtt("am2"), Store.sectionRate("am2"), cAccent))
            sc.addView(rateRow("午前Ⅰ (共通)", Store.sectionAtt("am1"), Store.sectionRate("am1"), 0xFF15A6A0.toInt()))
            col.addView(sc)

            // 年度別
            val yc = card()
            yc.addView(tv("年度別 正答率", 14f, cText, true))
            yc.addView(spacer(10))
            yc.addView(tv("午前Ⅱ", 12.5f, cGold, true)); yc.addView(spacer(6))
            for ((year, _) in QuestionData.am2ByYear)
                yc.addView(rateRow(year, Store.yearAtt("am2", year), Store.yearRate("am2", year), cAccent))
            yc.addView(spacer(4))
            yc.addView(tv("午前Ⅰ", 12.5f, cGold, true)); yc.addView(spacer(6))
            for ((year, _) in QuestionData.am1ByYear)
                yc.addView(rateRow(year, Store.yearAtt("am1", year), Store.yearRate("am1", year), 0xFF15A6A0.toInt()))
            col.addView(yc)

            // 分野別 正答率(挑戦済みのみ、弱い順)
            val cstats = categoryStats()
            val tried = cstats.entries
                .filter { it.value[0] > 0 }
                .sortedBy { it.value[1].toDouble() / it.value[0] }
            if (tried.isNotEmpty()) {
                val cc = card()
                cc.addView(tv("分野別 正答率", 14f, cText, true))
                cc.addView(spacer(4))
                cc.addView(tv("弱い順。タップで集中演習", 12f, cSub))
                cc.addView(spacer(10))
                for (e in tried) {
                    val att = e.value[0]; val cor = e.value[1]
                    val r = cor * 100 / att
                    val row = rateRow("${e.key} ($cor/$att)", att, r,
                        if (r < 50) cRed else if (r < 70) cGold else cGreen)
                    row.setOnClickListener { startCategoryDrill(e.key) }
                    cc.addView(row)
                }
                col.addView(cc)
            }

            // 分野を選んで集中演習(全分野)
            val dc = card()
            dc.addView(tv("分野を選んで集中演習", 14f, cText, true))
            dc.addView(spacer(4))
            dc.addView(tv("苦手分野をピンポイントで反復", 12f, cSub))
            dc.addView(spacer(10))
            val catCounts = Category.counts()
            for (cat in Category.all) {
                val n = catCounts[cat] ?: 0
                if (n == 0) continue
                dc.addView(listButton(cat, "${n}問") { startCategoryDrill(cat) })
            }
            col.addView(dc)

            // よく間違える問題
            val weak = Store.weakQuestionIds(8).mapNotNull { resolveId(it) }
            if (weak.isNotEmpty()) {
                val wc = card()
                wc.addView(tv("よく間違える問題", 14f, cText, true))
                wc.addView(spacer(4))
                wc.addView(tv("タップで再挑戦", 12f, cSub))
                wc.addView(spacer(8))
                for ((section, q) in weak) {
                    wc.addView(listButton("${sectionLabel(section)} ${q.year} 問${q.no}",
                        q.text.take(28) + if (q.text.length > 28) "…" else "") {
                        showQuestion(section, q, "再挑戦", { showAnalysis() }) { showAnalysis() }
                    })
                }
                col.addView(wc)
            }
        })
    }

    // ============================================================
    // タブ4: マイページ
    // ============================================================
    private fun showMyPage() {
        setContent(screen("マイページ") { col ->
            val c = card()
            c.addView(tv("学習の記録", 14f, cGold, true))
            c.addView(spacer(10))
            c.addView(kv("連続学習日数", "${Store.streak()}日"))
            c.addView(kv("最長連続", "${Store.bestStreak()}日"))
            c.addView(kv("学習した日数", "${Store.studyDays()}日"))
            c.addView(kv("総回答数", "${Store.totalAnswered()}問"))
            c.addView(kv("正解数", "${Store.totalCorrect()}問"))
            c.addView(kv("総合正答率", "${Store.overallRate()}%"))
            col.addView(c)

            val c2 = card()
            c2.addView(tv("収録状況", 14f, cGold, true))
            c2.addView(spacer(10))
            c2.addView(kv("午前Ⅱ", "${QuestionData.am2ByYear.size}年度 / ${countQ("am2")}問"))
            c2.addView(kv("午前Ⅰ", "${QuestionData.am1ByYear.size}年度 / ${countQ("am1")}問"))
            c2.addView(kv("午後PDF", "${QuestionData.pmExams.size}回分"))
            c2.addView(kv("用語辞典", "${Glossary.terms.size}語"))
            col.addView(c2)

            val c3 = card()
            c3.addView(tv("設定", 14f, cGold, true))
            c3.addView(spacer(10))
            val hintRow = LinearLayout(this)
            hintRow.orientation = LinearLayout.HORIZONTAL
            hintRow.gravity = Gravity.CENTER_VERTICAL
            val hintLabel = LinearLayout(this)
            hintLabel.orientation = LinearLayout.VERTICAL
            hintLabel.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            hintLabel.addView(tv("ヒントを常に表示", 14.5f, cText, true))
            hintLabel.addView(tv("午前の問題でヒントを最初から表示(模試を除く)", 12f, cSub))
            hintRow.addView(hintLabel)
            val toggle = tv(if (Store.hintAlways()) "ON" else "OFF", 13f, Color.WHITE, true)
            toggle.background = rounded(if (Store.hintAlways()) cGreen else cCard2, 20)
            toggle.setPadding(dp(16), dp(6), dp(16), dp(6))
            toggle.setOnClickListener {
                val nv = !Store.hintAlways()
                Store.setHintAlways(nv)
                toggle.text = if (nv) "ON" else "OFF"
                toggle.background = rounded(if (nv) cGreen else cCard2, 20)
            }
            hintRow.addView(toggle)
            c3.addView(hintRow)
            col.addView(c3)

            col.addView(listButton("このアプリについて") { showAbout() })
            col.addView(listButton("学習データをリセット", "回答履歴・連続日数などを全消去") { confirmReset() })
        })
    }

    private fun kv(k: String, v: String): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(8)
        row.layoutParams = lp
        val l = tv(k, 14f, cSub)
        l.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(l)
        row.addView(tv(v, 14f, cText, true))
        return row
    }

    private fun showAbout() {
        setContent(screen("このアプリについて", back = { showMyPage() }) { col ->
            val c = card()
            c.addView(tv("情報処理安全確保支援士(SC)の合格を目指す学習アプリです。", 14f, cText))
            c.addView(spacer(10))
            c.addView(tv("・過去問はIPA公開の問題・解答例に基づき、正解は公式解答例と全問照合済み。", 13f, cSub))
            c.addView(spacer(6))
            c.addView(tv("・午前Ⅱ/午前Ⅰは4択自動採点、午後は公式PDFを参照。", 13f, cSub))
            c.addView(spacer(6))
            c.addView(tv("・学習記録は端末内(オフライン)にのみ保存されます。", 13f, cSub))
            col.addView(c)
        })
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("学習データをリセット")
            .setMessage("回答履歴・正答率・連続日数・復習キューをすべて削除します。よろしいですか？")
            .setPositiveButton("削除する") { _, _ -> Store.resetAll(); showMyPage() }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // ============================================================
    // 外部リンク
    // ============================================================
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setMessage("リンクを開けませんでした。\n$url")
                .setPositiveButton("OK", null).show()
        }
    }
}
