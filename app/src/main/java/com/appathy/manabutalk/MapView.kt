package com.appathy.manabutalk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 知識マップを描画するView。放射状レイアウト＋ピンチズーム＋ドラッグ移動。
 * 中央=情報処理安全確保支援士 / 第1階層=大分類(=演習の分野) / 第2階層=中分類。
 * 大分類タップで onCategoryTap(分野名) を呼ぶ。
 */
@SuppressLint("ViewConstructor")
class MapView(
    ctx: Context,
    private val onCategoryTap: (String) -> Unit
) : View(ctx) {

    private val cBg = 0xFF0E1116.toInt()
    private val cCenter = 0xFF2EA6FF.toInt()
    private val cMid = 0xFF222C3A.toInt()
    private val cText = 0xFFECF1F8.toInt()
    private val cSub = 0xFF97A6B6.toInt()
    private val cLine = 0xFF314052.toInt()
    private val cFlow = 0xFFE6B450.toInt()

    // 大分類ごとの色(学習順で寒色→暖色)
    private val catColors = listOf(
        0xFF3D7EA6.toInt(), 0xFF2E8B87.toInt(), 0xFF3DA35D.toInt(), 0xFF6E9B34.toInt(),
        0xFF9B8C2E.toInt(), 0xFFB07430.toInt(), 0xFFB0553A.toInt(), 0xFF9B4A6E.toInt(),
        0xFF6E4A9B.toInt(), 0xFF55606E.toInt()
    )

    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cText; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val pSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cSub; textAlign = Paint.Align.CENTER
    }

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var moved = false

    // ヒット判定用(描画時に記録): 中心座標と半径と分野名
    private data class Hit(val x: Float, val y: Float, val r: Float, val cat: String)
    private val hits = ArrayList<Hit>()

    private val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            scale = (scale * d.scaleFactor).coerceIn(0.45f, 3.0f)
            moved = true
            invalidate()
            return true
        }
    })

    private val branches = KnowledgeMap.inLearningOrder()

    /** マップ全体の論理サイズ(この座標系で描いてから scale/off で変換) */
    private val world = 1400f

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(cBg)
        hits.clear()

        val cx = width / 2f + offX
        val cy = height / 2f + offY
        // 画面に収まる初期倍率
        val base = (minOf(width, height) / world) * 1.9f
        val s = base * scale

        val n = branches.size
        val ringR = world * 0.30f * s          // 大分類までの距離
        val midR = world * 0.46f * s           // 中分類までの距離

        // --- 前提→発展の矢印(大分類間) ---
        pLine.color = cFlow
        pLine.strokeWidth = 2.2f * s * 0.9f
        pLine.alpha = 150
        val angleOf = HashMap<String, Double>()
        for (i in branches.indices) {
            val a = -Math.PI / 2 + 2 * Math.PI * i / n
            angleOf[branches[i].category] = a
        }
        for ((from, to) in KnowledgeMap.prereq) {
            val a1 = angleOf[from] ?: continue
            val a2 = angleOf[to] ?: continue
            val x1 = cx + (ringR * cos(a1)).toFloat()
            val y1 = cy + (ringR * sin(a1)).toFloat()
            val x2 = cx + (ringR * cos(a2)).toFloat()
            val y2 = cy + (ringR * sin(a2)).toFloat()
            // 中心寄りに膨らませた曲線
            val mx = (x1 + x2) / 2f
            val my = (y1 + y2) / 2f
            val ctrlX = cx + (mx - cx) * 0.45f
            val ctrlY = cy + (my - cy) * 0.45f
            val path = Path()
            path.moveTo(x1, y1)
            path.quadTo(ctrlX, ctrlY, x2, y2)
            canvas.drawPath(path, pLine)
            // 矢印
            drawArrow(canvas, ctrlX, ctrlY, x2, y2, s)
        }
        pLine.alpha = 255

        // --- 中心から大分類への線 + ノード ---
        for (i in branches.indices) {
            val b = branches[i]
            val a = angleOf[b.category]!!
            val x = cx + (ringR * cos(a)).toFloat()
            val y = cy + (ringR * sin(a)).toFloat()
            val color = catColors[i % catColors.size]

            pLine.color = cLine
            pLine.strokeWidth = 3f * s
            canvas.drawLine(cx, cy, x, y, pLine)

            // 中分類(大分類の外側に扇状)
            val mids = b.mid
            val spread = (2 * Math.PI / n) * 0.78
            for (j in mids.indices) {
                val ma = a - spread / 2 + spread * (j + 0.5) / mids.size
                val mx = cx + (midR * cos(ma)).toFloat()
                val my = cy + (midR * sin(ma)).toFloat()
                pLine.color = cLine
                pLine.strokeWidth = 1.6f * s
                canvas.drawLine(x, y, mx, my, pLine)

                pFill.color = cMid
                val mw = 62f * s
                val mh = 20f * s
                canvas.drawRoundRect(mx - mw / 2, my - mh / 2, mx + mw / 2, my + mh / 2, 6f * s, 6f * s, pFill)
                pSmall.textSize = 9.5f * s
                pSmall.color = cText
                canvas.drawText(ellipsize(mids[j].name, 8), mx, my + 3.4f * s, pSmall)
            }

            // 大分類ノード
            val r = 40f * s
            pFill.color = color
            canvas.drawCircle(x, y, r, pFill)
            pLine.color = Color.WHITE
            pLine.strokeWidth = 1.5f * s
            pLine.alpha = 90
            canvas.drawCircle(x, y, r, pLine)
            pLine.alpha = 255

            pText.textSize = 11.5f * s
            pText.color = Color.WHITE
            val lines = wrap(b.category, 6)
            val startY = y - (lines.size - 1) * 6.5f * s + 4f * s
            for ((k, ln) in lines.withIndex())
                canvas.drawText(ln, x, startY + k * 13f * s, pText)
            // 学習順バッジ
            pSmall.textSize = 9f * s
            pSmall.color = 0xFFFFE6A8.toInt()
            canvas.drawText("${b.order}", x, y - r - 5f * s, pSmall)

            hits.add(Hit(x, y, r * 1.15f, b.category))
        }

        // --- 中心ノード ---
        val cr = 54f * s
        pFill.color = cCenter
        canvas.drawCircle(cx, cy, cr, pFill)
        pText.textSize = 12.5f * s
        pText.color = Color.WHITE
        val t = listOf("情報処理", "安全確保", "支援士")
        for ((k, ln) in t.withIndex())
            canvas.drawText(ln, cx, cy - 10f * s + k * 15f * s, pText)
    }

    private fun drawArrow(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, s: Float) {
        val ang = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
        val len = 9f * s
        val back = 46f * s   // ノード半径ぶん手前で止める
        val ex = toX - (back * cos(ang)).toFloat()
        val ey = toY - (back * sin(ang)).toFloat()
        val p = Path()
        p.moveTo(ex, ey)
        p.lineTo(ex - (len * cos(ang - 0.42)).toFloat(), ey - (len * sin(ang - 0.42)).toFloat())
        p.lineTo(ex - (len * cos(ang + 0.42)).toFloat(), ey - (len * sin(ang + 0.42)).toFloat())
        p.close()
        pFill.color = cFlow
        canvas.drawPath(p, pFill)
    }

    private fun wrap(s: String, per: Int): List<String> {
        if (s.length <= per) return listOf(s)
        val res = ArrayList<String>()
        var i = 0
        while (i < s.length) {
            res.add(s.substring(i, minOf(i + per, s.length)))
            i += per
        }
        return res
    }

    private fun ellipsize(s: String, max: Int) = if (s.length <= max) s else s.substring(0, max) + "…"

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

    fun resetView() {
        scale = 1f; offX = 0f; offY = 0f; invalidate()
    }
}
