# 支援士ゼミ (情報処理安全確保支援士 学習アプリ) HANDOFF

## 概要
情報処理安全確保支援士(SC)合格を目指す学習アプリ。旧「まなぶトーク」から方針転換し、
設計書「情報処理安全確保支援士_スマホ版_設計書.md」の5タブ構成に沿って再構築(v2.0)。
過去問データ(QuestionData.kt / 270問)はそのまま資産として継続利用。

- package: com.appathy.manabutalk (リポジトリ名維持のため据え置き。表示名は「支援士ゼミ」)
- ビルド: AGP/Kotlin/Gradle は従来設定。minSdk 26 / compile+target 34 / Java 17。
- XMLレイアウト不使用、標準Activity、外部依存なし(SharedPreferencesのみ)。ダークテーマ。

## ファイル構成 (app/src/main/java/com/appathy/manabutalk/)
- QuestionData.kt … 過去問データ(不変の資産)。data class Question / PmExam。
  - am2ByYear: 午前Ⅱ(セキュリティ) 6年度×25問=150問
  - am1ByYear: 午前Ⅰ(共通) 4年度×30問=120問
  - pmExams: 午後PDF(令和7秋〜令和5春)
  - 正解はすべてIPA公式解答例と全問照合済み。
- Store.kt … 学習記録の永続化・分析(SharedPreferences "sc_store")。
  - recordAnswer(section,year,no,correct): 全体/セクション別/年度別/問題別集計、復習キュー、
    忘却曲線ボックス(0-5→0,1,3,7,14,30日後due)、学習日・連続日数。
  - recordFlash(id,remembered): 単語帳用。忘却曲線のみ更新(正答率統計に不算入)。
  - 参照: overallRate/sectionRate/yearRate/streak/bestStreak/studyDays/wrongIds/dueIds/
    weakQuestionIds/passEstimate/resetAll。
  - section識別子: "am2"=午前Ⅱ, "am1"=午前Ⅰ。問題ID = "section|year|no"。
- Glossary.kt … 用語辞典(約50語、SC頻出語を簡潔解説)。
- Category.kt … 問題文キーワードによる分野自動分類(10分野)。categoryOf/pool/counts/all。
  既存Question構造を変えずに分野別分析・集中演習を実現。分類は優先順キーワードマッチ。
- MainActivity.kt … 5タブUI本体(プログラマティックKotlin)。

## 画面(設計書対応)
- ボトムタブ: ホーム / 学習 / AI / 分析 / マイページ
- ホーム: 時間帯挨拶・連続日数・正答率、クイック学習(10問)、復習、模試、AIおすすめ(弱点分野)。
- 学習: 午前Ⅱ/午前Ⅰの年度選択→テスト形式(順番採点)/ランダム、午後PDF、単語帳、用語辞典。
- 問題演習: 4択タップで自動採点(緑/赤)・解説表示・Storeへ記録。
- 単語帳: 問題→答えのフラッシュカード。覚えた/まだで忘却曲線更新。dueを優先出題。
- 用語辞典: 検索付き。
- AI: 解説動画検索(YouTube ACTION_VIEW)、午後の自己添削導線、端末内AI(Bonsai)連携・OCR・音声は「準備中」。
- 分析: 合格可能性の目安、セクション別/年度別正答率(バー)、よく間違える問題(再挑戦導線)。
- マイページ: 学習記録、収録状況、このアプリについて、データリセット(確認ダイアログ)。

## 未実装(設計書にあり今後対応)
- 端末内AIチャット(Bonsai連携。ユーザー側Bonsaiで別途進行)、OCR学習、音声学習、知識グラフ、
  通知、ウィジェット。→ UI上は「準備中」と明示。

## 注意
- 学習データは端末内のみ(オフライン)。リセットはマイページから。
- 午後は記述式のため自動採点せず公式PDF参照+自己採点。

## 版履歴(抜粋)
- 〜v1.7: 旧「まなぶトーク」。過去問270問を整備(午前Ⅱ150/午前Ⅰ120、午後PDF)。
- v2.0: 設計書に沿って5タブ学習アプリへ全面再構築。Store/Glossary新設、学習分析・復習・
  忘却曲線・単語帳・用語辞典・合格可能性の目安を実装。表示名を「支援士ゼミ」に変更、ダークテーマ化。
- v2.1: (1)分野タグ導入=Category.ktで全270問を自動分類。分析タブに分野別正答率＋分野別集中演習、
  ホームのおすすめを弱点分野ベースに。(2)模試を本格化=時間計測(午前Ⅱ40分/午前Ⅰ50分)・見直し
  フラグ★・結果に分野別スコアと解答時間・見直し一覧を表示。(3)午後 自己採点シート=公式PDFを
  参照しつつ設問ごとに○△×で自己採点し達成度を集計(模範解答要点は捏造せず公式解答例PDFを参照)。
  StoreにqAtt/qCorを追加(分野集計用)。端末内AI(Bonsai)連携は保留。
