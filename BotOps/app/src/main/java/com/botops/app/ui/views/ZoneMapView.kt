package com.botops.app.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.botops.app.data.entities.WaypointEntity
import kotlin.math.roundToInt

class ZoneMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63")
        style = Paint.Style.FILL
    }

    private val selectedPointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DE91E63")
        style = Paint.Style.FILL
    }

    private val existingWaypointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
        style = Paint.Style.FILL
    }

    var minCoordX: Double = 0.0
    var maxCoordX: Double = 100.0
    var minCoordY: Double = 0.0
    var maxCoordY: Double = 100.0

    private var selectedPoint: PointF? = null
    private var existingWaypoints: List<WaypointEntity> = emptyList()

    var onCoordinateSelectedListener: ((Double, Double) -> Unit)? = null

    fun setZoneBounds(minX: Double, maxX: Double, minY: Double, maxY: Double) {
        this.minCoordX = minX
        this.maxCoordX = maxX.coerceAtLeast(minX + 1.0)
        this.minCoordY = minY
        this.maxCoordY = maxY.coerceAtLeast(minY + 1.0)
        invalidate()
    }

    fun setExistingWaypoints(waypoints: List<WaypointEntity>) {
        this.existingWaypoints = waypoints
        invalidate()
    }

    fun setSelectedCoordinate(x: Double, y: Double) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val px = mapX(x, w)
        val py = mapY(y, h)
        selectedPoint = PointF(px, py)
        invalidate()
    }

    private fun mapX(x: Double, w: Float): Float {
        return (((x - minCoordX) / (maxCoordX - minCoordX)) * w).toFloat().coerceIn(0f, w)
    }

    private fun mapY(y: Double, h: Float): Float {
        return ((1.0 - ((y - minCoordY) / (maxCoordY - minCoordY))) * h).toFloat().coerceIn(0f, h)
    }

    private fun unmapX(px: Float, w: Float): Double {
        return minCoordX + (px / w) * (maxCoordX - minCoordX)
    }

    private fun unmapY(py: Float, h: Float): Double {
        return minCoordY + (1.0 - (py / h)) * (maxCoordY - minCoordY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0 || h <= 0) return

        val gridCols = 10
        val gridRows = 10
        val colWidth = w / gridCols
        val rowHeight = h / gridRows

        for (i in 0..gridCols) {
            val x = i * colWidth
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
        for (j in 0..gridRows) {
            val y = j * rowHeight
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        canvas.drawRect(0f, 0f, w, h, borderPaint)

        for (wp in existingWaypoints) {
            val wx = mapX(wp.xCord, w)
            val wy = mapY(wp.yCord, h)
            canvas.drawCircle(wx, wy, 12f, existingWaypointPaint)
            canvas.drawText(wp.name, wx + 16f, wy + 8f, textPaint)
        }

        selectedPoint?.let { pt ->
            canvas.drawCircle(pt.x, pt.y, 30f, selectedPointOuterPaint)
            canvas.drawCircle(pt.x, pt.y, 14f, selectedPointPaint)

            val coordX = unmapX(pt.x, w).roundTo1Decimal()
            val coordY = unmapY(pt.y, h).roundTo1Decimal()

            val text = "Selected: ($coordX, $coordY)"
            canvas.drawText(text, (pt.x + 20f).coerceAtMost(w - 220f), (pt.y - 20f).coerceAtLeast(40f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val px = event.x.coerceIn(0f, width.toFloat())
            val py = event.y.coerceIn(0f, height.toFloat())

            selectedPoint = PointF(px, py)
            invalidate()

            val coordX = unmapX(px, width.toFloat()).roundTo1Decimal()
            val coordY = unmapY(py, height.toFloat()).roundTo1Decimal()

            onCoordinateSelectedListener?.invoke(coordX, coordY)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun Double.roundTo1Decimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }
}
