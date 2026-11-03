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
        color = Color.DKGRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63") // Vibrant Pink/Red for selection
        style = Paint.Style.FILL
    }

    private val selectedPointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DE91E63") // Semi-transparent ring
        style = Paint.Style.FILL
    }

    private val existingWaypointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3") // Blue for existing waypoints
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
        style = Paint.Style.FILL
    }

    // Grid bounds default: 0.0 to 100.0 x 0.0 to 100.0
    var maxCoordX: Double = 100.0
    var maxCoordY: Double = 100.0

    private var selectedPoint: PointF? = null
    private var existingWaypoints: List<WaypointEntity> = emptyList()

    var onCoordinateSelectedListener: ((Double, Double) -> Unit)? = null

    fun setExistingWaypoints(waypoints: List<WaypointEntity>) {
        this.existingWaypoints = waypoints
        invalidate()
    }

    fun setSelectedCoordinate(x: Double, y: Double) {
        val px = ((x / maxCoordX) * width).toFloat()
        val py = ((1.0 - (y / maxCoordY)) * height).toFloat()
        selectedPoint = PointF(px, py)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0 || h <= 0) return

        // Draw background grid (10x10 grid lines)
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

        // Outer border
        canvas.drawRect(0f, 0f, w, h, borderPaint)

        // Draw existing waypoints
        for (wp in existingWaypoints) {
            val wx = ((wp.xCord / maxCoordX) * w).toFloat()
            val wy = ((1.0 - (wp.yCord / maxCoordY)) * h).toFloat()
            canvas.drawCircle(wx, wy, 12f, existingWaypointPaint)
            canvas.drawText(wp.name, wx + 16f, wy + 8f, textPaint)
        }

        // Draw selected coordinate marker if present
        selectedPoint?.let { pt ->
            canvas.drawCircle(pt.x, pt.y, 30f, selectedPointOuterPaint)
            canvas.drawCircle(pt.x, pt.y, 14f, selectedPointPaint)

            // Calculate coordinate values
            val coordX = ((pt.x / w) * maxCoordX).roundTo1Decimal()
            val coordY = ((1.0 - (pt.y / h)) * maxCoordY).roundTo1Decimal()

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

            val coordX = ((px / width.toFloat()) * maxCoordX).roundTo1Decimal()
            val coordY = ((1.0 - (py / height.toFloat())) * maxCoordY).roundTo1Decimal()

            onCoordinateSelectedListener?.invoke(coordX, coordY)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun Double.roundTo1Decimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }
}
