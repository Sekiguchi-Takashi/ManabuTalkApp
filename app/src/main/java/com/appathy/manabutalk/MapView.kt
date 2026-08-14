package com.appathy.manabutalk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 知識マップ描画View。
 * 中心=試験全体 / 内側リング=大分類(=演習の分野) / 外側=中分類ラベル。
 * 重なりを避けるため、大分類は角度を等分し、中分類はその扇の中に均等配置する。
 * 金の弧＝前提→発展。ピンチズーム・ドラッグ移動、大分類タップでコールバック。
 */
@SuppressLint("ViewConstructor")
class MapView(
    ctx: Context,
    private val onCategoryTap: (String) -> Unit
) : View(ctx) {

    private val cBg = 0xFF0E1116.toInt()
    private val cCenter = 0xFF2EA6FF.toInt()
    private val cMidBox = 0xFF1B2431.toInt()
    private val cText = 0xFFECF1F8.toInt()
    private val cSub = 0xFFA8B6C6.toInt()
    private val cLine = 0xFF2C3949.toInt()
    private val cFlow = 0xFFE6B450.toInt()

    private val catColors = listOf(
        0xFF3D7EA6.toInt(), 0xFF2E8B87.toInt(), 0xFF3DA35D.toInt(), 0xFF6E9B34.toInt(),
        0xFF9B8C2E.toInt(), 0xFFB07430.toInt(), 0xFFB0553A.toInt(), 0xFF9B4A6E.toInt(),
        0xFF6E4A9B.toInt(), 0xFF55606E.toInt()
    )

    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cText; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val pMid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cText; textAlign = Paint.Align.CENTER
    }

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var moved = false

    private data class Hit(val x: Float, val y: Float, val r: Float, val cat: String)
    private val hits = ArrayList<Hit>()

    private val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            scale = (scale * d.scaleFactor).coerceIn(0.5f, 3.5f)
            moved = true
            invalidate()
            return true
        }
    })

    private val branches = KnowledgeMap.inLearningOrder()

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(cBg)
        hits.clear()

        val cx = width / 2f + offX
        val cy = height / 2f + offY
        // 画面短辺を基準にした単位。全体が収まるよう控えめに。
        val u = minOf(width, height) / 1000f * scale

        val n = branches.size
        val rCat = 300f * u     // 大分類リング半径
        val rMid = 470f * u     // 中分類ラベル半径
        val catR = 62f * u      // 大分類ノード半径

        val angleOf = HashMap<String, Double>()
        for (i in branches.indices) {
            // 真上から時計回りに等分
            angleOf[branches[i].category] = -Math.PI / 2 + 2 * Math.PI * i / n
        }

        // --- 前提→発展: 中心寄りの弧で描き、ノードと重ならないようにする ---
        pStroke.color = cFlow
        pStroke.strokeWidth = 3.2f * u
        pStroke.alpha = 130
        for ((from, to) in KnowledgeMap.prereq) {
            val a1 = angleOf[from] ?: continue
            val a2 = angleOf[to] ?: continue
            val rIn = rCat - catR - 10f * u
            val x1 = cx + (rIn * cos(a1)).toFloat()
            val y1 = cy + (rIn * sin(a1)).toFloat()
            val x2 = cx + (rIn * cos(a2)).toFloat()
            val y2 = cy + (rIn * sin(a2)).toFloat()
            val mx = (x1 + x2) / 2f
            val my = (y1 + y2) / 2f
            val ctrlX = cx + (mx - cx) * 0.35f
            val ctrlY = cy + (my - cy) * 0.35f
            val path = Path()
            path.moveTo(x1, y1)
            path.quadTo(ctrlX, ctrlY, x2, y2)
            canvas.drawPath(path, pStroke)
            drawArrow(canvas, ctrlX, ctrlY, x2, y2, u)
        }
        pStroke.alpha = 255

        // --- 大分類と中分類 ---
        for (i in branches.indices) {
            val b = branches[i]
            val a = angleOf[b.category]!!
            val x = cx + (rCat * cos(a)).toFloat()
            val y = cy + (rCat * sin(a)).toFloat()
            val color = catColors[i % catColors.size]

            // 中心 → 大分類
            pStroke.color = cLine
            pStroke.strokeWidth = 4f * u
            canvas.drawLine(cx, cy, x, y, pStroke)

            // 中分類(扇内に均等配置。隣の分野と被らない幅に制限)
            val mids = b.mid
            val slot = 2 * Math.PI / n
            val spread = slot * 0.72
            for (j in mids.indices) {
                val ma = if (mids.size == 1) a
                         else a - spread / 2 + spread * j / (mids.size - 1)
                val mx = cx + (rMid * cos(ma)).toFloat()
                val my = cy + (rMid * sin(ma)).toFloat()

                pStroke.color = cLine
                pStroke.strokeWidth = 1.8f * u
                canvas.drawLine(
                    x + (catR * cos(ma)).toFloat() * 0.35f,
                    y + (catR * sin(ma)).toFloat() * 0.35f,
                    mx, my, pStroke
                )

                // ラベル箱(文字幅に合わせる)
                pMid.textSize = 17f * u
                val label = mids[j].name
                val tw = pMid.measureText(label)
                val bw = tw + 16f * u
                val bh = 26f * u
                val rect = RectF(mx - bw / 2, my - bh / 2, mx + bw / 2, my + bh / 2)
                pFill.color = cMidBox
                canvas.drawRoundRect(rect, 7f * u, 7f * u, pFill)
                pStroke.color = color
                pStroke.strokeWidth = 1.6f * u
                canvas.drawRoundRect(rect, 7f * u, 7f * u, pStroke)
                pMid.color = cText
                canvas.drawText(label, mx, my + 6f * u, pMid)

                // 用語数バッジ
                if (mids[j].terms.isNotEmpty()) {
                    pMid.textSize = 12f * u
                    pMid.color = cSub
                    canvas.drawText("${mids[j].terms.size}語", mx, my + bh / 2 + 14f * u, pMid)
                }
            }

            // 大分類ノード
            pFill.color = color
            canvas.drawCircle(x, y, catR, pFill)
            pStroke.color = Color.WHITE
            pStroke.strokeWidth = 2f * u
            pStroke.alpha = 80
            canvas.drawCircle(x, y, catR, pStroke)
            pStroke.alpha = 255

            pText.textSize = 19f * u
            pText.color = Color.WHITE
            val lines = wrapJa(b.category, 5)
            val startY = y - (lines.size - 1) * 11f * u + 7f * u
            for ((k, ln) in lines.withIndex())
                canvas.drawText(ln, x, startY + k * 22f * u, pText)

            // 学習順バッジ(円の外側)
            val bx = x + (catR * 0.95f * cos(a - 0.6)).toFloat()
            val by = y + (catR * 0.95f * sin(a - 0.6)).toFloat()
            pFill.color = cFlow
            canvas.drawCircle(bx, by, 15f * u, pFill)
            pText.textSize = 16f * u
            pText.color = 0xFF231A05.toInt()
            canvas.drawText("${b.order}", bx, by + 6f * u, pText)

            hits.add(Hit(x, y, catR * 1.1f, b.category))
        }

        // --- 中心 ---
        val cr = 86f * u
        pFill.color = cCenter
        canvas.drawCircle(cx, cy, cr, pFill)
        pStroke.color = Color.WHITE
        pStroke.strokeWidth = 2.5f * u
        pStroke.alpha = 70
        canvas.drawCircle(cx, cy, cr, pStroke)
        pStroke.alpha = 255
        pText.textSize = 21f * u
        pText.color = Color.WHITE
        val t = listOf("情報処理", "安全確保", "支援士")
        for ((k, ln) in t.withIndex())
            canvas.drawText(ln, cx, cy - 16f * u + k * 24f * u, pText)
    }

    private fun drawArrow(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, u: Float) {
        val ang = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
        val len = 14f * u
        val p = Path()
        p.moveTo(toX, toY)
        p.lineTo(toX - (len * cos(ang - 0.42)).toFloat(), toY - (len * sin(ang - 0.42)).toFloat())
        p.lineTo(toX - (len * cos(ang + 0.42)).toFloat(), toY - (len * sin(ang + 0.42)).toFloat())
        p.close()
        pFill.color = cFlow
        canvas.drawPath(p, pFill)
    }

    /** 日本語を指定文字数で折り返す(中黒や長音での不自然な分割は避けない簡易版) */
    private fun wrapJa(s: String, per: Int): List<String> {
        if (s.length <= per) return listOf(s)
        val res = ArrayList<String>()
        var i = 0
        while (i < s.length) {
            res.add(s.substring(i, minOf(i + per, s.length)))
            i += per
        }
        return res
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y; dragging = true; moved = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && !scaleDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (hypot(dx.toDouble(), dy.toDouble()) > 6) moved = true
                    offX += dx; offY += dy
                    lastX = event.x; lastY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                dragging = false
                if (!moved) {
                    for (h in hits) {
                        if (hypot((event.x - h.x).toDouble(), (event.y - h.y).toDouble()) <= h.r) {
                            onCategoryTap(h.cat); break
                        }
                    }
                }
            }
        }
        return true
    }

    fun resetView() { scale = 1f; offX = 0f; offY = 0f; invalidate() }
    fun zoom(f: Float) { scale = (scale * f).coerceIn(0.5f, 3.5f); invalidate() }
}
