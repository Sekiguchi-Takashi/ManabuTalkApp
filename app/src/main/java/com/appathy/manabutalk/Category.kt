package com.appathy.manabutalk

/**
 * 問題の分野を、問題文のキーワードから自動分類する。
 * 既存のQuestionデータ構造を変えずに分野別分析・集中演習を実現するための仕組み。
 * 優先順位順(先にマッチしたものを採用)にキーワードを評価する。
 */
object Category {

    // 表示名(この順で分析に並ぶ)
    val all = listOf(
        "暗号技術",
        "認証・PKI",
        "メール",
        "Web・アプリ",
        "マルウェア・攻撃",
        "ネットワーク",
        "セキュリティ管理・制度",
        "システム・DB・開発",
        "マネジメント・監査・経営",
        "その他"
    )

    // 評価順(上から順にマッチ判定)。上位ほど具体的・優先。
    private val rules: List<Pair<String, List<String>>> = listOf(
        "メール" to listOf(
            "DKIM", "DMARC", "OP25B", "S/MIME", "APOP", "SMTP", "POP3", "IMAP",
            "電子メール", "メールサーバ", "迷惑メール", "サブミッションポート", "SPF"
        ),
        "セキュリティ管理・制度" to listOf(
            "ISMS", "27000", "27001", "27017", "CSIRT", "PSIRT", "SOC", "SIEM", "SOAR",
            "CASB", "CSPM", "UEBA", "ゼロトラスト", "ISMAP", "NIST", "サイバーセキュリティフレームワーク",
            "CSF", "キルチェーン", "ATT&CK", "MITRE", "CVE", "CVSS", "CWE", "CPE", "IoC",
            "SBOM", "インシデント", "フォレンジック", "証拠保全", "NOTICE", "JVN", "FIPS",
            "リスク特定", "リスク対応", "リスクアプローチ", "SIM3", "Open CSIRT", "IoC",
            "脆弱性管理", "SCAP", "脅威インテリジェンス", "成熟度"
        ),
        "Web・アプリ" to listOf(
            "クロスサイト", "XSS", "CSRF", "SQLインジェクション", "OSコマンドインジェクション",
            "WAF", "cookie", "セッション", "クリックジャッキング", "Ajax", "HSTS", "HTTP",
            "ステータスコード", "HTMLフォーム", "Webアプリ", "Webサイト", "Webサーバ", "リバースプロキシ",
            "X-Frame-Options", "Referer", "OAuth", "SAML", "SSO", "シングルサインオン", "3Dセキュア"
        ),
        "マルウェア・攻撃" to listOf(
            "マルウェア", "ランサム", "ボット", "Mirai", "DDoS", "DoS", "サービス妨害",
            "Pass the Hash", "Smurf", "DRDoS", "リフレクション", "リフレクション攻撃", "増幅",
            "サイドチャネル", "タイミング攻撃", "MITB", "コネクトバック", "エクスプロイト",
            "ディープフェイク", "クリプトジャッキング", "カミンスキー", "ポイズニング", "フィッシング",
            "標的型", "ドメインフロンティング", "モデルインバージョン", "Adversarial", "NTPリフレクション",
            "SYN", "攻撃", "踏み台", "水飲み場", "なりすまし"
        ),
        "認証・PKI" to listOf(
            "PKI", "認証局", "証明書", "デジタル署名", "電子署名", "RA", "VA", "CRL", "OCSP",
            "X.509", "CPS", "FIDO", "パスキー", "802.1X", "RADIUS", "Diameter", "Kerberos",
            "認証デバイス", "生体認証", "虹彩", "Identity", "多要素", "ワンタイムパスワード",
            "XMLデジタル署名", "XML Signature", "属性証明書", "本人確認", "サプリカント", "オーセンティケータ",
            "MAC", "メッセージ認証", "AAA"
        ),
        "暗号技術" to listOf(
            "暗号", "ハッシュ", "鍵", "PQC", "CRYPTREC", "AES", "RSA", "SHA", "量子",
            "ワンタイムパッド", "楕円", "CTR", "共通鍵", "公開鍵", "DH鍵", "衝突発見", "情報理論的",
            "耐量子", "暗号利用モード", "SHA-512"
        ),
        "ネットワーク" to listOf(
            "DNSSEC", "DNS", "TCP", "UDP", "IPアドレス", "IPv4", "IPv6", "IPsec", "VPN",
            "L2TP", "PPTP", "ルーティング", "OSPF", "RIP", "サブネット", "ポート番号", "ARP",
            "ICMP", "スパニングツリー", "ブロードキャスト", "マルチキャスト", "LAN", "パケット",
            "プロトコル", "光ファイバ", "ネットワークタップ", "CSMA", "PoE", "帯域", "DHCP",
            "DTLS", "TLS", "SSH", "OSI", "イーサネット", "コリジョン", "DNS CAA", "CAA"
        ),
        "マネジメント・監査・経営" to listOf(
            "プロジェクト", "スクラム", "アジャイル", "EVM", "スケジュール", "クリティカルパス",
            "クラッシング", "ファストトラッキング", "サービスマネジメント", "SLA", "可用性",
            "問題管理", "インシデント管理", "監査", "内部統制", "統制", "DX", "SOA", "ERP",
            "BPR", "PPM", "SWOT", "ファイブフォース", "損益", "変動費", "固定費", "著作権",
            "不正競争", "労働者派遣", "意匠", "契約", "派遣", "財務", "20000", "リスクアプローチ",
            "システム監査", "ウォークスルー", "フォローアップ", "KPT", "レトロスペクティブ",
            "TCPI", "CPI", "SPI", "PBP", "バランススコアカード", "戦略マップ", "アンゾフ",
            "コ・クリエーション", "ダーウィンの海", "プログラムマネジメント", "プロビジョニング",
            "EMS", "フィージビリティ", "タックマン", "DX認定", "デザインレビュー", "所要量"
        ),
        "システム・DB・開発" to listOf(
            "アルゴリズム", "スタック", "キュー", "計算量", "CPU", "キャッシュ", "命令",
            "稼働率", "MTBF", "MTTR", "RAID", "仮想記憶", "ページ", "コンパイラ", "正規形",
            "SQL", "データベース", "DBMS", "トランザクション", "ビュー", "外部キー", "候補キー",
            "UML", "オブジェクト指向", "デザインパターン", "テスト", "論理回路", "IoT",
            "センサー", "アクチュエーター", "フォント", "命令セット", "VLIW", "ハミング",
            "逆ポーランド", "待ち行列", "M/M/1", "量子ゲート", "エッジコンピューティング",
            "エッジAI", "機械学習", "過学習", "交差検証", "ストアドプロシージャ", "データディクショナリ",
            "カオスエンジニアリング", "GoF", "形式手法", "SVG", "信頼性設計", "デッドロック",
            "マシンビジョン", "MES", "SBOM", "2正規形", "3正規形"
        )
    )

    fun categoryOf(q: QuestionData.Question): String {
        val hay = q.text + " " + (q.choices?.joinToString(" ") ?: "")
        for ((cat, kws) in rules) {
            for (k in kws) if (hay.contains(k, ignoreCase = true)) return cat
        }
        // フォールバックは説明文でもう一度だけ判定
        for ((cat, kws) in rules) {
            for (k in kws) if (q.explanation.contains(k, ignoreCase = true)) return cat
        }
        return "その他"
    }

    /** 分野→(section,問題) のプール。分野別の集中演習に使う。 */
    fun pool(cat: String): List<Pair<String, QuestionData.Question>> {
        val res = ArrayList<Pair<String, QuestionData.Question>>()
        for (q in QuestionData.am2ByYear.values.flatten()) if (categoryOf(q) == cat) res.add("am2" to q)
        for (q in QuestionData.am1ByYear.values.flatten()) if (categoryOf(q) == cat) res.add("am1" to q)
        return res
    }

    /** 各分野の収録数(全問走査。分析画面のヘッダ用) */
    fun counts(): LinkedHashMap<String, Int> {
        val m = LinkedHashMap<String, Int>()
        for (c in all) m[c] = 0
        for (q in QuestionData.am2ByYear.values.flatten()) m[categoryOf(q)] = (m[categoryOf(q)] ?: 0) + 1
        for (q in QuestionData.am1ByYear.values.flatten()) m[categoryOf(q)] = (m[categoryOf(q)] ?: 0) + 1
        return m
    }
}
