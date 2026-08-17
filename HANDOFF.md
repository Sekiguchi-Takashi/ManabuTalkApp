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
- Glossary.kt … 用語辞典(約155語)。data class Term(term, reading, full=正式名称, desc=意味, etym=語源, cat=分野)。
  categories=分野一覧。略語は正式名称・語源つき。用語集2(略語)・用語集3(分野別)＋SC頻出を統合。
- KnowledgeMap.kt … 知識マップのデータ。Branch(category=Category.allと一致, glossaryCat, order=推奨学習順,
  summary, mid=中分類→small=小分類/terms=関連用語)。用語辞典155語すべてを中分類に紐づけ済み、prereq=前提→発展の関係。byCategory/inLearningOrder。
- MapView.kt … 知識マップ描画View(Canvas)。放射状レイアウト、ピンチズーム/ドラッグ移動、
  大分類タップでコールバック。中心=試験全体、第1階層=分野(学習順バッジ)、第2階層=中分類、金の矢印=前提→発展。
- Hint.kt … 解説文から「答えに近づくヒント」を自動生成。正解の記号(ア〜エ)と正解選択肢の語・先頭名詞句を
  伏せ、先頭文中心に着眼点を残す。of(q)を返す。270問の定義は不変。
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

## 納品規約(恒久)
1. deploy.sh は「add/commit → pull --rebase → push → タグ発行」まで1コマンドで完結。
   次タグは `git fetch --tags --force` → `git tag --list 'v*' | sort -V | tail -1` から算出し、
   `git tag <名>` → `git push origin <名>` でローカル発行する。
   GitHub API の heads/releases 参照は反映遅延で一つ前のコミットにタグが付くため禁止。
   第2引数に `notag` を渡すと push のみでタグを発行しない。
2. build.yml は作らない・同梱しない。CI は release.yml(タグ起動)のみ。
   `actions/upload-artifact` は使わない(Artifacts枠0.5GBが枯渇し全ビルドが落ちる)。
3. `ci/` ディレクトリと `.github/workflows/release.yml` は配布ビルドに必要。削除・追跡解除しない
   (ZIPにも含めず、リポジトリ側の実体をそのまま残す)。
4. ファイルを削除する納品では deploy.sh に `rm -f 対象パス` を足す
   (`unzip -o` は端末の旧ファイルを消さないため)。本納品では build.yml を削除するので
   deploy.sh に `rm -f .github/workflows/build.yml` を含めている。
5. 納品はバージョン番号付きZIP＋同メッセージに実行4行ブロック。冒頭に【本番】か【テスト】を明示。
   シェルは echo 禁止・対話入力禁止・トークンをチャットに貼らせない。
   `git pull --rebase` は必須(カタログ管理システムが API 経由で release.yml と ci/appathy.keystore を
   直接コミットするため、無いと push が rejected)。

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
- v2.2: 用語辞典を大幅拡充(約50→155語)。アップロードの用語集2(略語+正式名称+語源)・用語集3(分野別
  用語)を取り込み、SC頻出語と統合。Termに full/etym/cat を追加。用語辞典に分野フィルタ(チップ)、
  正式名称・語源の表示、暗記用「用語カード」(表=用語→裏=意味+語源)を追加。分類・模試等は据え置き。
- v2.3: 用語カードを2モード化。「単語→意味」(表=用語→裏=意味+語源)と「意味→単語」(表=意味→裏=用語+
  正式名称+語源)を用語辞典から起動できる。分野フィルタ・検索で絞ったまま暗記可能。
- v2.4: 午前問題にヒント機能。解説からHint.ktが答えを伏せたヒントを生成。既定は「ヒントを見る」ボタンで
  表示、マイページの設定「ヒントを常に表示」でONにすると最初から表示。模試(allowHint=false)はヒント無し。
  StoreにhintAlways設定を追加(リセットしても設定は保持)。
- v2.5: 知識マップを実装(学習タブ→知識マップ)。中心「情報処理安全確保支援士」から10分野(=Category.all)へ
  放射状に展開し、中分類まで描画。金の矢印で前提→発展、円の数字で推奨学習順を表示。ピンチズーム/ドラッグ対応。
  分野タップ→詳細(概要・自分の正答率・前提/発展分野・中分類/小分類一覧)から、その分野の集中演習や
  用語辞典(対応分野で絞込み)へ直結。補助として「学習順ガイド」も追加。演習・分析・辞典とマップが連動。
- v2.6: 知識マップを整頓＋用語紐づけ。描画を刷新(大分類の角度を等分、中分類は扇内に均等配置して重なりを解消、
  ラベルは文字幅に合わせた枠、学習順バッジを円外に、前提→発展の弧を内側に退避)。ズーム＋/－/リセット追加。
  KnowledgeMap.Midにtermsを追加し、Glossaryの155語すべてを41の中分類へ分配(過不足なし)。分野詳細では
  中分類ごとに用語チップを表示し、タップで意味・正式名称・語源をダイアログ表示→辞典へも遷移可能。
- v2.6(納品時): deploy.sh を恒久仕様で同梱(pull --rebase とタグ発行を含む)。REPO=ManabuTalkApp。
- v2.6(納品時): build.yml から upload-artifact ステップを削除(Artifacts枠枯渇によるビルド失敗を回避)。
- v2.7: 納品規約に準拠。deploy.sh をローカルタグ方式(git tag --list | sort -V → git tag → git push origin タグ)
  に変更し、notag オプションと `rm -f .github/workflows/build.yml` を追加。build.yml は同梱を廃止(CIは
  release.yml のタグ起動のみ)。アプリ機能の変更はなし。
