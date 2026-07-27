package com.appathy.manabutalk

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.net.URLEncoder
import java.util.Calendar

/**
 * ManabuTalk (まなぶトーク) v1.0
 *
 * AI対話形式学習アプリのプロトタイプ。
 * 起動 -> 挨拶+雑談 -> 5教科(情報のみ有効) -> 3モード(教えてもらう/問題を解く/テストする)
 * -> テーマ/範囲/コメント入力 -> 「教えてもらう」はYouTube検索へリンク
 *
 * 依存ライブラリゼロ / XMLレイアウトなし / 全画面Kotlinプログラマティック構築
 */
class MainActivity : Activity() {

    // ---- 配色 ----
    private val colorBg = Color.parseColor("#12102A")
    private val colorCard = Color.parseColor("#1F1B45")
    private val colorAccent = Color.parseColor("#00C2A8")
    private val colorAccentDim = Color.parseColor("#2E2A5C")
    private val colorTextMain = Color.parseColor("#FFFFFF")
    private val colorTextSub = Color.parseColor("#B8B4E0")
    private val colorDisabled = Color.parseColor("#3A3660")

    // ---- 学習データ定義 ----
    // 教科ごとにテーマ一覧を持つ(将来拡張用)。現状は情報/セキュリティのみ実装。
    private val subjectThemes = linkedMapOf(
        "国語" to listOf<String>(),
        "算数" to listOf<String>(),
        "理科" to listOf<String>(),
        "社会" to listOf<String>(),
        "情報" to listOf("セキュリティ", "午前Ⅰ(共通)")
    )
    private val modes = listOf("教えてもらう", "問題を解く", "テストする")
    private val ranges = listOf("広い", "中間", "狭い")

    // ---- 状態 ----
    private var currentSubject: String? = null
    private var currentMode: String? = null
    private var selectedTheme: String? = null
    private var selectedRange: String? = null

    // ---- 雑談ネタ(時間帯別。将来AI生成に差し替え予定) ----
    private val morningTalks = listOf(
        "早起きだね。今日はどんな一日にする?",
        "朝ごはんはもう食べた?",
        "今日の調子はどう?"
    )
    private val afternoonTalks = listOf(
        "午後もぼちぼちいこう。",
        "お昼は何食べた?",
        "ちょっと一息入れる?"
    )
    private val eveningTalks = listOf(
        "今日一日おつかれさま。",
        "夜はゆっくりできそう?",
        "今日あった出来事、なんか話す?"
    )
    private val chatReplies = listOf(
        "うんうん、そうなんだ。",
        "なるほどね。",
        "へえ、面白いね!",
        "それでそれで?",
        "いいね、その調子。"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showGreetingScreen()
    }

    // ============================================================
    // 共通ヘルパー
    // ============================================================

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun sp(v: Int): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v.toFloat(), resources.displayMetrics)

    private fun rootLayout(): LinearLayout {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(colorBg)
        root.setPadding(dp(20), dp(36), dp(20), dp(20))
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        return root
    }

    private fun titleText(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextColor(colorTextMain)
        t.textSize = 22f
        t.setTypeface(null, Typeface.BOLD)
        t.setPadding(0, 0, 0, dp(16))
        return t
    }

    private fun backButton(onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = "← もどる"
        b.setTextColor(colorTextSub)
        b.setBackgroundColor(Color.TRANSPARENT)
        b.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        b.layoutParams = lp
        return b
    }

    private fun primaryButton(label: String): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(Color.WHITE)
        b.setBackgroundColor(colorAccent)
        b.setPadding(dp(24), dp(16), dp(24), dp(16))
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(12)
        b.layoutParams = lp
        return b
    }

    private fun setButtonEnabled(b: Button, enabled: Boolean) {
        b.isEnabled = enabled
        b.setBackgroundColor(if (enabled) colorAccent else colorDisabled)
        b.setTextColor(if (enabled) Color.WHITE else colorTextSub)
    }

    // ============================================================
    // 画面1: 挨拶 + 雑談
    // ============================================================

    private fun showGreetingScreen() {
        val root = rootLayout()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (greeting, talkPool) = when {
            hour in 5..10 -> "おはよう!" to morningTalks
            hour in 11..16 -> "こんにちは!" to afternoonTalks
            else -> "こんばんは!" to eveningTalks
        }
        val opener = talkPool.random()

        root.addView(titleText("$greeting"))

        // 吹き出し風カード
        val bubble = TextView(this)
        bubble.text = opener
        bubble.setTextColor(colorTextMain)
        bubble.textSize = 16f
        bubble.setBackgroundColor(colorCard)
        bubble.setPadding(dp(16), dp(16), dp(16), dp(16))
        val bubbleLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        bubbleLp.bottomMargin = dp(16)
        bubble.layoutParams = bubbleLp
        root.addView(bubble)

        // 会話ログ表示エリア
        val chatLogScroll = ScrollView(this)
        val chatLogLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        chatLogScroll.layoutParams = chatLogLp
        val chatLog = LinearLayout(this)
        chatLog.orientation = LinearLayout.VERTICAL
        chatLogScroll.addView(chatLog)
        root.addView(chatLogScroll)

        // 入力欄 + 送信ボタン
        val inputRow = LinearLayout(this)
        inputRow.orientation = LinearLayout.HORIZONTAL
        inputRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val input = EditText(this)
        input.hint = "話しかけてみる..."
        input.setTextColor(colorTextMain)
        input.setHintTextColor(colorTextSub)
        input.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val sendBtn = Button(this)
        sendBtn.text = "送信"
        sendBtn.setTextColor(Color.WHITE)
        sendBtn.setBackgroundColor(colorAccentDim)
        sendBtn.setOnClickListener {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) {
                addChatLine(chatLog, "あなた", msg, colorTextSub)
                addChatLine(chatLog, "AI", chatReplies.random(), colorTextMain)
                input.setText("")
                chatLogScroll.post { chatLogScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
        inputRow.addView(input)
        inputRow.addView(sendBtn)
        root.addView(inputRow)

        // 勉強するボタン(常時表示)
        val studyBtn = primaryButton("べんきょうする")
        studyBtn.setOnClickListener { showSubjectScreen() }
        root.addView(studyBtn)

        setContentView(root)
    }

    private fun addChatLine(container: LinearLayout, speaker: String, text: String, color: Int) {
        val line = TextView(this)
        line.text = "$speaker: $text"
        line.setTextColor(color)
        line.textSize = 14f
        line.setPadding(0, dp(4), 0, dp(4))
        container.addView(line)
    }

    // ============================================================
    // 画面2: 5教科選択(情報のみ有効)
    // ============================================================

    private fun showSubjectScreen() {
        val root = rootLayout()
        root.addView(backButton { showGreetingScreen() })
        root.addView(titleText("なにを べんきょうする?"))

        for ((subject, themes) in subjectThemes) {
            val enabled = themes.isNotEmpty()
            val b = Button(this)
            b.text = if (enabled) subject else "$subject (じゅんびちゅう)"
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            b.layoutParams = lp
            b.setPadding(dp(20), dp(18), dp(20), dp(18))
            if (enabled) {
                b.setBackgroundColor(colorCard)
                b.setTextColor(colorTextMain)
                b.setOnClickListener {
                    currentSubject = subject
                    showModeScreen()
                }
            } else {
                b.setBackgroundColor(colorDisabled)
                b.setTextColor(colorTextSub)
                b.isEnabled = false
            }
            root.addView(b)
        }

        setContentView(root)
    }

    // ============================================================
    // 画面3: 3モード選択(教えてもらう/問題を解く/テストする)
    // ============================================================

    private fun showModeScreen() {
        val root = rootLayout()
        root.addView(backButton { showSubjectScreen() })
        root.addView(titleText("${currentSubject} - どうやって べんきょうする?"))

        for (mode in modes) {
            val b = Button(this)
            b.text = mode
            b.setBackgroundColor(colorCard)
            b.setTextColor(colorTextMain)
            b.setPadding(dp(20), dp(18), dp(20), dp(18))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            b.layoutParams = lp
            b.setOnClickListener {
                currentMode = mode
                selectedTheme = null
                selectedRange = null
                when (mode) {
                    "教えてもらう" -> showFormScreen()
                    "問題を解く" -> showSolveThemeScreen()
                    "テストする" -> showTestThemeScreen()
                    else -> showFormScreen()
                }
            }
            root.addView(b)
        }

        setContentView(root)
    }

    // ============================================================
    // 画面4: テーマ/範囲/コメント入力 + 実行ボタン
    // ============================================================

    private fun showFormScreen() {
        val root = rootLayout()
        root.addView(backButton { showModeScreen() })
        root.addView(titleText("${currentMode}"))

        val themeList = subjectThemes[currentSubject] ?: listOf()

        // --- テーマ選択 ---
        root.addView(fieldLabel("テーマ"))
        val themeSpinner = Spinner(this)
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeList)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter
        themeSpinner.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        root.addView(themeSpinner)

        // --- 範囲選択 ---
        root.addView(fieldLabel("範囲"))
        val rangeGroup = RadioGroup(this)
        rangeGroup.orientation = RadioGroup.HORIZONTAL
        val radioButtons = mutableListOf<RadioButton>()
        for (r in ranges) {
            val rb = RadioButton(this)
            rb.text = r
            rb.setTextColor(colorTextMain)
            rangeGroup.addView(rb)
            radioButtons.add(rb)
        }
        root.addView(rangeGroup)

        // --- コメント入力(20文字以内) ---
        root.addView(fieldLabel("コメント (じゆうきさい・20文字まで)"))
        val commentInput = EditText(this)
        commentInput.hint = "例: パスワードの作り方"
        commentInput.setTextColor(colorTextMain)
        commentInput.setHintTextColor(colorTextSub)
        commentInput.filters = arrayOf(InputFilter.LengthFilter(20))
        commentInput.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        root.addView(commentInput)

        val counter = TextView(this)
        counter.text = "0/20"
        counter.setTextColor(colorTextSub)
        counter.textSize = 12f
        counter.gravity = Gravity.END
        root.addView(counter)

        // --- 実行ボタン ---
        val actionBtn = primaryButton(currentMode ?: "実行")
        setButtonEnabled(actionBtn, false)
        root.addView(actionBtn)

        fun validate() {
            val hasTheme = themeSpinner.selectedItem != null
            val hasRange = radioButtons.any { it.isChecked }
            val hasComment = commentInput.text.toString().trim().isNotEmpty()
            setButtonEnabled(actionBtn, hasTheme && hasRange && hasComment)
        }

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedTheme = themeList.getOrNull(pos)
                validate()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        for (rb in radioButtons) {
            rb.setOnClickListener {
                selectedRange = rb.text.toString()
                validate()
            }
        }
        commentInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                counter.text = "${s?.length ?: 0}/20"
                validate()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        actionBtn.setOnClickListener {
            val comment = commentInput.text.toString().trim()
            when (currentMode) {
                "教えてもらう" -> openYoutubeSearch(selectedTheme ?: "", selectedRange ?: "", comment)
                else -> Toast.makeText(
                    this, "この機能は開発中です。次のアップデートをお楽しみに!", Toast.LENGTH_SHORT
                ).show()
            }
        }

        setContentView(root)
    }

    private fun fieldLabel(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextColor(colorTextSub)
        t.textSize = 13f
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(16)
        lp.bottomMargin = dp(4)
        t.layoutParams = lp
        return t
    }

    // ============================================================
    // テーマ選択(セキュリティ=午前Ⅱ / 午前Ⅰ共通)
    // ============================================================

    private fun themeButton(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.setBackgroundColor(colorCard)
        b.setTextColor(colorTextMain)
        b.isAllCaps = false
        b.setPadding(dp(20), dp(18), dp(20), dp(18))
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(10)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    // 「問題を解く」→ テーマ選択
    private fun showSolveThemeScreen() {
        val root = rootLayout()
        root.addView(backButton { showModeScreen() })
        root.addView(titleText("問題を解く - テーマを選ぶ"))
        root.addView(themeButton("セキュリティ (午前Ⅱ)") {
            val pool = QuestionData.am2ByYear.values.flatten()
            showRandomFromPool(pool) { showSolveThemeScreen() }
        })
        root.addView(themeButton("午前Ⅰ(共通)") {
            val pool = QuestionData.am1ByYear.values.flatten()
            showRandomFromPool(pool) { showSolveThemeScreen() }
        })
        setContentView(root)
    }

    private fun showRandomFromPool(pool: List<QuestionData.Question>, onBack: () -> Unit) {
        if (pool.isEmpty()) return
        val q = pool.random()
        showQuestionScreen(q, onBack = onBack, onNext = { showRandomFromPool(pool, onBack) }, nextLabel = "次の問題")
    }

    // 「テストする」→ テーマ選択
    private fun showTestThemeScreen() {
        val root = rootLayout()
        root.addView(backButton { showModeScreen() })
        root.addView(titleText("テスト - テーマを選ぶ"))
        root.addView(themeButton("セキュリティ (午前Ⅱ・午後)") { showTestKindScreen() })
        root.addView(themeButton("午前Ⅰ(共通)") { showAm1YearScreen() })
        setContentView(root)
    }

    // ---- 午前Ⅰ: 年度選択 → 順番に出題 ----
    private fun showAm1YearScreen() {
        val root = rootLayout()
        root.addView(backButton { showTestThemeScreen() })
        root.addView(titleText("午前Ⅰ(共通) - 年度を選ぶ"))
        for ((year, list) in QuestionData.am1ByYear) {
            root.addView(themeButton("$year (${list.size}問)") { showAm1TestQuestion(year, list, 0) })
        }
        setContentView(root)
    }

    private fun showAm1TestQuestion(year: String, list: List<QuestionData.Question>, index: Int) {
        if (index >= list.size) {
            showAm1TestDone()
            return
        }
        val q = list[index]
        showQuestionScreen(
            q,
            onBack = { showAm1YearScreen() },
            onNext = { showAm1TestQuestion(year, list, index + 1) },
            nextLabel = if (index + 1 >= list.size) "結果へ" else "次へ (${index + 2}/${list.size})"
        )
    }

    private fun showAm1TestDone() {
        val root = rootLayout()
        root.addView(titleText("午前Ⅰ(共通) おつかれさま!"))
        val msg = TextView(this)
        msg.text = "全問終了しました。もう一度挑戦するか、別のメニューを選べます。"
        msg.setTextColor(colorTextMain)
        msg.textSize = 15f
        msg.setBackgroundColor(colorCard)
        msg.setPadding(dp(16), dp(16), dp(16), dp(16))
        root.addView(msg)
        val again = primaryButton("年度選択にもどる")
        again.setOnClickListener { showAm1YearScreen() }
        root.addView(again)
        val home = primaryButton("さいしょの画面へ")
        home.setOnClickListener { showGreetingScreen() }
        root.addView(home)
        setContentView(root)
    }

    // ============================================================
    // 「問題を解く」-> 過去問からランダムに1問出題
    // ============================================================

    private fun showRandomQuestionScreen() {
        val pool = QuestionData.am2ByYear.values.flatten()
        val q = pool.random()
        showQuestionScreen(q, onBack = { showModeScreen() }, onNext = { showRandomQuestionScreen() }, nextLabel = "次の問題")
    }

    /**
     * 1問を表示する共通画面。
     * choices があれば4択タップで自動採点、なければ「答えを見る」で正解＋解説を表示。
     */
    private fun showQuestionScreen(
        q: QuestionData.Question,
        onBack: () -> Unit,
        onNext: () -> Unit,
        nextLabel: String
    ) {
        val root = rootLayout()
        root.addView(backButton { onBack() })

        val header = TextView(this)
        header.text = "${q.year} 午前Ⅱ 問${q.no}"
        header.setTextColor(colorTextSub)
        header.textSize = 13f
        header.setPadding(0, 0, 0, dp(8))
        root.addView(header)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        scroll.addView(content)
        root.addView(scroll)

        val question = TextView(this)
        question.text = q.text
        question.setTextColor(colorTextMain)
        question.textSize = 16f
        question.setBackgroundColor(colorCard)
        question.setPadding(dp(16), dp(16), dp(16), dp(16))
        val qLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        qLp.bottomMargin = dp(12)
        question.layoutParams = qLp
        content.addView(question)

        // 結果・解説表示エリア
        val resultBox = TextView(this)
        resultBox.setTextColor(colorTextMain)
        resultBox.textSize = 15f
        resultBox.setPadding(dp(16), dp(16), dp(16), dp(16))
        resultBox.visibility = View.GONE
        val rLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        rLp.topMargin = dp(12)
        resultBox.layoutParams = rLp

        val labels = listOf("ア", "イ", "ウ", "エ")

        fun revealExplanation() {
            resultBox.visibility = View.VISIBLE
            resultBox.text = "正解: ${q.answerText}\n\n【解説】\n${q.explanation}"
            resultBox.setBackgroundColor(colorAccentDim)
        }

        if (q.choices != null) {
            // 4択・自動採点
            val choiceButtons = mutableListOf<Button>()
            var answered = false
            for ((i, c) in q.choices.withIndex()) {
                val b = Button(this)
                b.text = "${labels[i]}. $c"
                b.setTextColor(colorTextMain)
                b.setBackgroundColor(colorCard)
                b.setPadding(dp(16), dp(14), dp(16), dp(14))
                b.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                b.isAllCaps = false
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                b.layoutParams = lp
                b.setOnClickListener {
                    if (answered) return@setOnClickListener
                    answered = true
                    for ((j, bb) in choiceButtons.withIndex()) {
                        when (j) {
                            q.answerIndex -> bb.setBackgroundColor(Color.parseColor("#1E7D5A"))
                            i -> if (i != q.answerIndex) bb.setBackgroundColor(Color.parseColor("#8A2C2C"))
                        }
                    }
                    revealExplanation()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
                choiceButtons.add(b)
                content.addView(b)
            }
            content.addView(resultBox)
        } else {
            // 一問一答・自己採点(答えを見る)
            content.addView(resultBox)
            val showAnsBtn = primaryButton("答えを見る")
            showAnsBtn.setOnClickListener {
                revealExplanation()
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
            content.addView(showAnsBtn)
        }

        // 下部: 次の問題
        val nextBtn = primaryButton(nextLabel)
        nextBtn.setOnClickListener { onNext() }
        root.addView(nextBtn)

        setContentView(root)
    }

    // ============================================================
    // 「テストする」-> 午前Ⅱ / 午後 の選択
    // ============================================================

    private fun showTestKindScreen() {
        val root = rootLayout()
        root.addView(backButton { showTestThemeScreen() })
        root.addView(titleText("テスト - どちらを受ける?"))

        val am2 = Button(this)
        am2.text = "午前Ⅱ (4択・年度別)"
        am2.setBackgroundColor(colorCard)
        am2.setTextColor(colorTextMain)
        am2.setPadding(dp(20), dp(18), dp(20), dp(18))
        val lp1 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp1.topMargin = dp(10)
        am2.layoutParams = lp1
        am2.setOnClickListener { showAm2YearScreen() }
        root.addView(am2)

        val pm = Button(this)
        pm.text = "午後 (記述・年度別PDF)"
        pm.setBackgroundColor(colorCard)
        pm.setTextColor(colorTextMain)
        pm.setPadding(dp(20), dp(18), dp(20), dp(18))
        val lp2 = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp2.topMargin = dp(10)
        pm.layoutParams = lp2
        pm.setOnClickListener { showPmYearScreen() }
        root.addView(pm)

        setContentView(root)
    }

    // ---- 午前Ⅱ: 年度選択 ----
    private fun showAm2YearScreen() {
        val root = rootLayout()
        root.addView(backButton { showTestKindScreen() })
        root.addView(titleText("午前Ⅱ - 年度を選ぶ"))

        for ((year, list) in QuestionData.am2ByYear) {
            val b = Button(this)
            b.text = "$year (${list.size}問)"
            b.setBackgroundColor(colorCard)
            b.setTextColor(colorTextMain)
            b.setPadding(dp(20), dp(18), dp(20), dp(18))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            b.layoutParams = lp
            b.setOnClickListener { startAm2Test(year, list) }
            root.addView(b)
        }

        setContentView(root)
    }

    // ---- 午前Ⅱ: 年度内を順番に出題 ----
    private fun startAm2Test(year: String, list: List<QuestionData.Question>) {
        showAm2TestQuestion(year, list, 0)
    }

    private fun showAm2TestQuestion(year: String, list: List<QuestionData.Question>, index: Int) {
        if (index >= list.size) {
            showAm2TestDone(year)
            return
        }
        val q = list[index]
        showQuestionScreen(
            q,
            onBack = { showAm2YearScreen() },
            onNext = { showAm2TestQuestion(year, list, index + 1) },
            nextLabel = if (index + 1 >= list.size) "結果へ" else "次へ (${index + 2}/${list.size})"
        )
    }

    private fun showAm2TestDone(year: String) {
        val root = rootLayout()
        root.addView(titleText("$year 午前Ⅱ おつかれさま!"))
        val msg = TextView(this)
        msg.text = "全問終了しました。もう一度挑戦するか、別の年度を選べます。"
        msg.setTextColor(colorTextMain)
        msg.textSize = 15f
        msg.setBackgroundColor(colorCard)
        msg.setPadding(dp(16), dp(16), dp(16), dp(16))
        root.addView(msg)

        val again = primaryButton("年度選択にもどる")
        again.setOnClickListener { showAm2YearScreen() }
        root.addView(again)

        val home = primaryButton("さいしょの画面へ")
        home.setOnClickListener { showGreetingScreen() }
        root.addView(home)

        setContentView(root)
    }

    // ---- 午後: 年度選択 -> 公式PDFを開く ----
    private fun showPmYearScreen() {
        val root = rootLayout()
        root.addView(backButton { showTestKindScreen() })
        root.addView(titleText("午後 - 年度を選ぶ"))

        val note = TextView(this)
        note.text = "午後は記述式のため、IPA公式の問題PDFを開きます(自己採点)。"
        note.setTextColor(colorTextSub)
        note.textSize = 12f
        note.setPadding(0, 0, 0, dp(8))
        root.addView(note)

        for (exam in QuestionData.pmExams) {
            val qBtn = Button(this)
            qBtn.text = "${exam.label} - 問題PDF"
            qBtn.setBackgroundColor(colorCard)
            qBtn.setTextColor(colorTextMain)
            qBtn.setPadding(dp(20), dp(16), dp(20), dp(16))
            qBtn.isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            qBtn.layoutParams = lp
            qBtn.setOnClickListener { openUrl(exam.questionsPdf) }
            root.addView(qBtn)

            val aBtn = Button(this)
            aBtn.text = "${exam.label} - 解答例PDF"
            aBtn.setBackgroundColor(colorAccentDim)
            aBtn.setTextColor(colorTextMain)
            aBtn.setPadding(dp(20), dp(12), dp(20), dp(12))
            aBtn.isAllCaps = false
            val lp2 = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp2.topMargin = dp(4)
            aBtn.layoutParams = lp2
            aBtn.setOnClickListener { openUrl(exam.answersPdf) }
            root.addView(aBtn)
        }

        setContentView(root)
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    // ============================================================
    // 「教えてもらう」-> YouTube検索
    // ============================================================

    private fun openYoutubeSearch(theme: String, range: String, comment: String) {
        val query = listOf(theme, range, comment, "解説")
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val encoded = URLEncoder.encode(query, "UTF-8")
        val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}
