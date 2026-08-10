package com.appathy.manabutalk

/**
 * 知識マップのデータ。既存の分野体系(Category.all)と連動させる。
 *
 * 構造: 中心「情報処理安全確保支援士」→ 大分類(=演習の分野) → 中分類 → 小分類
 * prereq: 「前提となる知識 → 発展する知識」の関係(大分類間)。学習順序の推測に使う。
 */
object KnowledgeMap {

    /** 小分類まで持つノード。glossaryCat は用語辞典のフィルタ名(対応するもののみ)。 */
    data class Branch(
        val category: String,          // Category.all と一致(演習・分析と連動)
        val glossaryCat: String,       // Glossary.categories と対応(なければ "すべて")
        val order: Int,                // 推奨学習順(1が最初)
        val summary: String,           // この分野で押さえること
        val mid: List<Mid>
    )

    data class Mid(val name: String, val small: List<String>)

    /** 前提 → 発展 (大分類間の依存関係) */
    val prereq: List<Pair<String, String>> = listOf(
        "システム・DB・開発" to "ネットワーク",
        "ネットワーク" to "暗号技術",
        "暗号技術" to "認証・PKI",
        "認証・PKI" to "Web・アプリ",
        "ネットワーク" to "メール",
        "暗号技術" to "メール",
        "Web・アプリ" to "マルウェア・攻撃",
        "ネットワーク" to "マルウェア・攻撃",
        "マルウェア・攻撃" to "セキュリティ管理・制度",
        "セキュリティ管理・制度" to "マネジメント・監査・経営"
    )

    val branches: List<Branch> = listOf(
        Branch(
            "システム・DB・開発", "クラウド・システム", 1,
            "午前Ⅰ共通の土台。コンピュータの基本とデータ構造・DB・開発プロセスを押さえる。",
            listOf(
                Mid("基礎理論", listOf("アルゴリズム", "計算量", "待ち行列", "論理回路")),
                Mid("コンピュータ構成", listOf("CPU・CPI", "キャッシュ", "仮想記憶", "稼働率・MTBF")),
                Mid("データベース", listOf("正規化", "SQL", "トランザクション", "ビュー・外部キー")),
                Mid("開発", listOf("UML", "デザインパターン", "テスト技法", "アジャイル・スクラム")),
                Mid("クラウド・仮想化", listOf("コンテナ", "ハイパーバイザ", "フェイルオーバー", "スケーリング"))
            )
        ),
        Branch(
            "ネットワーク", "ネットワーク", 2,
            "攻撃も防御もネットワーク上で起きる。プロトコルの層と役割を先に固める。",
            listOf(
                Mid("プロトコル基礎", listOf("OSI参照モデル", "TCP/UDP", "IPv4/IPv6", "ポート番号")),
                Mid("名前解決", listOf("DNS", "DNSSEC", "キャッシュポイズニング対策")),
                Mid("LAN・経路制御", listOf("ARP", "ICMP", "サブネット", "OSPF/RIP", "スパニングツリー")),
                Mid("境界防御", listOf("ファイアウォール", "DMZ", "プロキシ", "IDS/IPS", "ネットワークタップ")),
                Mid("VPN・遠隔", listOf("IPsec", "SSH", "L2TP", "トンネリング"))
            )
        ),
        Branch(
            "暗号技術", "暗号・認証", 3,
            "機密性・完全性の土台。方式の違いと使いどころを区別できるようにする。",
            listOf(
                Mid("共通鍵暗号", listOf("AES", "暗号利用モード(CTR等)", "鍵配送問題")),
                Mid("公開鍵暗号", listOf("RSA", "楕円曲線暗号", "DH鍵共有")),
                Mid("ハッシュ・MAC", listOf("SHA", "衝突発見困難性", "HMAC", "ソルト・ストレッチング")),
                Mid("応用と将来", listOf("TLS1.3・AEAD", "PQC(耐量子)", "ワンタイムパッド", "CRYPTREC"))
            )
        ),
        Branch(
            "認証・PKI", "暗号・認証", 4,
            "暗号を使って「本人であること」を担保する仕組み。証明書の流れを理解する。",
            listOf(
                Mid("認証方式", listOf("多要素認証", "ワンタイムパスワード", "チャレンジレスポンス", "生体認証")),
                Mid("PKI", listOf("CA(発行)", "RA(本人確認)", "VA(失効検証)", "CRL/OCSP", "X.509")),
                Mid("デジタル署名", listOf("署名と検証", "XMLデジタル署名", "コードサイニング")),
                Mid("認証基盤", listOf("IEEE 802.1X", "RADIUS", "LDAP", "Kerberos", "FIDO/パスキー"))
            )
        ),
        Branch(
            "Web・アプリ", "Web・アプリ", 5,
            "攻撃対象になりやすい層。脆弱性と対策を一対一で覚える。",
            listOf(
                Mid("HTTP基礎", listOf("HTTP/HTTPS", "cookie属性", "セッション管理", "ステータスコード")),
                Mid("代表的脆弱性", listOf("XSS", "CSRF", "SQLインジェクション", "OSコマンドインジェクション", "ディレクトリトラバーサル")),
                Mid("対策", listOf("エスケープ処理", "プレースホルダ", "トークン照合", "X-Frame-Options", "HSTS", "WAF")),
                Mid("認可連携", listOf("OAuth 2.0", "SAML", "シングルサインオン"))
            )
        ),
        Branch(
            "メール", "メール", 6,
            "送信ドメイン認証は頻出。3つの役割分担を混同しないこと。",
            listOf(
                Mid("メールプロトコル", listOf("SMTP", "POP3", "IMAPS", "サブミッションポート587")),
                Mid("送信ドメイン認証", listOf("SPF(送信元IP)", "DKIM(署名)", "DMARC(失敗時の方針)")),
                Mid("迷惑メール対策", listOf("OP25B", "SMTP-AUTH")),
                Mid("メール暗号化", listOf("S/MIME", "PGP", "SMTP over TLS"))
            )
        ),
        Branch(
            "マルウェア・攻撃", "マルウェア・攻撃", 7,
            "攻撃の手口を知って初めて防御を選べる。手口→対策で結び付ける。",
            listOf(
                Mid("マルウェア", listOf("ランサムウェア", "ワーム", "ボット・ボットネット", "Mirai", "トロイの木馬")),
                Mid("サービス妨害", listOf("DoS/DDoS", "DRDoS(反射・増幅)", "SYNフラッド", "Smurf")),
                Mid("なりすまし・詐取", listOf("フィッシング", "中間者攻撃", "MITB", "Pass the Hash", "リプレイ攻撃")),
                Mid("侵入と潜伏", listOf("ゼロデイ攻撃", "標的型攻撃", "サプライチェーン攻撃", "バックドア", "コネクトバック")),
                Mid("攻撃の分析", listOf("サイバーキルチェーン", "MITRE ATT&CK", "IoC", "サイドチャネル攻撃"))
            )
        ),
        Branch(
            "セキュリティ管理・制度", "セキュリティ運用・管理", 8,
            "技術を組織として回す部分。用語の定義がそのまま問われる。",
            listOf(
                Mid("ISMS・リスク", listOf("JIS Q 27000", "リスク特定/分析/評価/対応", "リスクアセスメント")),
                Mid("組織と運用", listOf("CSIRT", "PSIRT", "SOC", "SIEM", "SOAR", "EDR")),
                Mid("脆弱性管理", listOf("CVE", "CVSS", "CWE", "CPE", "SBOM", "JVN")),
                Mid("インシデント対応", listOf("フォレンジックス", "証拠保全の順序", "ペネトレーションテスト")),
                Mid("制度・枠組み", listOf("ISMAP", "NIST CSF", "ゼロトラスト", "CASB/CSPM"))
            )
        ),
        Branch(
            "マネジメント・監査・経営", "その他", 9,
            "午前Ⅰで確実に点を取る領域。定義と手法名を対応付ける。",
            listOf(
                Mid("プロジェクト管理", listOf("EVM(CPI/SPI)", "クリティカルパス", "クラッシング", "ファストトラッキング")),
                Mid("サービス管理", listOf("SLA・可用性", "インシデント管理", "問題管理", "JIS Q 20000")),
                Mid("システム監査", listOf("監査手続", "ウォークスルー法", "フォローアップ", "IT全般統制/業務処理統制")),
                Mid("経営・法務", listOf("PPM", "ファイブフォース", "損益分岐点", "著作権", "不正競争防止法", "労働者派遣法"))
            )
        ),
        Branch(
            "その他", "その他", 10,
            "上記に収まらない基礎知識。",
            listOf(Mid("周辺知識", listOf("API", "RFC", "XML", "SQL基礎")))
        )
    )

    fun byCategory(cat: String): Branch? = branches.firstOrNull { it.category == cat }

    /** 学習順(order)で並べた大分類 */
    fun inLearningOrder(): List<Branch> = branches.sortedBy { it.order }
}
