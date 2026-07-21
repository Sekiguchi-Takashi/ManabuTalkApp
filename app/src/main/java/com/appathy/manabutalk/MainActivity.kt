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
        "情報" to listOf("セキュリティ")
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
                showFormScreen()
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
