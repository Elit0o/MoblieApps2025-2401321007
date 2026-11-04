package com.botops.app.ui.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.botops.app.data.entities.RobotEntity
import com.botops.app.data.entities.WaypointEntity
import com.botops.app.data.entities.ZoneEntity

class FleetLiveMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#2A2A2A") else Color.parseColor("#E0E0E0")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#1E293B") else Color.parseColor("#E3F2FD")
        style = Paint.Style.FILL
    }

    private val zoneBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#38BDF8") else Color.parseColor("#1976D2")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val waypointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#94A3B8") else Color.parseColor("#757575")
        style = Paint.Style.FILL
    }

    private val robotActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4ADE80")
        style = Paint.Style.FILL
    }

    private val robotIdlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    private val robotChargingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FB923C")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#E2E8F0") else Color.BLACK
        textSize = 28f
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isDarkMode) Color.parseColor("#38BDF8") else Color.parseColor("#1976D2")
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.FILL
    }

    private var zones: List<ZoneEntity> = emptyList()
    private var waypoints: List<WaypointEntity> = emptyList()
    private var robots: List<RobotEntity> = emptyList()

    private var minX = 0.0
    private var maxX = 100.0
    private var minY = 0.0
    private var maxY = 100.0

    fun setData(zones: List<ZoneEntity>, waypoints: List<WaypointEntity>, robots: List<RobotEntity>) {
        this.zones = zones
        this.waypoints = waypoints
        this.robots = robots

        if (zones.isNotEmpty()) {
            minX = zones.minOf { it.minX }
            maxX = zones.maxOf { it.maxX }.coerceAtLeast(minX + 10.0)
            minY = zones.minOf { it.minY }
            maxY = zones.maxOf { it.maxY }.coerceAtLeast(minY + 10.0)
        } else {
            minX = 0.0
            maxX = 100.0
            minY = 0.0
            maxY = 100.0
        }
        invalidate()
    }

    private fun mapX(x: Double, w: Float): Float {
        val span = (maxX - minX).coerceAtLeast(1.0)
        return (((x - minX) / span) * w).toFloat().coerceIn(0f, w)
    }

    private fun mapY(y: Double, h: Float): Float {
        val span = (maxY - minY).coerceAtLeast(1.0)
        return ((1.0 - ((y - minY) / span)) * h).toFloat().coerceIn(0f, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0 || h <= 0) return

        val cols = 10
        val rows = 10
        val cw = w / cols
        val rh = h / rows
        for (i in 0..cols) canvas.drawLine(i * cw, 0f, i * cw, h, gridPaint)
        for (j in 0..rows) canvas.drawLine(0f, j * rh, w, j * rh, gridPaint)

        for (zone in zones) {
            val left = mapX(zone.minX, w)
            val right = mapX(zone.maxX, w)
            val top = mapY(zone.maxY, h)
            val bottom = mapY(zone.minY, h)

            canvas.drawRect(left, top, right, bottom, zonePaint)
            canvas.drawRect(left, top, right, bottom, zoneBorderPaint)
            canvas.drawText("Room: ${zone.name}", left + 16f, top + 40f, titlePaint)
        }

        for (wp in waypoints) {
            val wx = mapX(wp.xCord, w)
            val wy = mapY(wp.yCord, h)
            canvas.drawCircle(wx, wy, 12f, waypointPaint)
            canvas.drawText(wp.name, wx + 16f, wy + 8f, textPaint)
        }

        for (robot in robots) {
            val rx = mapX(robot.currentX, w)
            val ry = mapY(robot.currentY, h)

            val paint = when {
                robot.status.equals("Active", true) -> robotActivePaint
                robot.status.equals("Charging", true) -> robotChargingPaint
                else -> robotIdlePaint
            }

            canvas.drawCircle(rx, ry, 24f, paint)
            canvas.drawText("${robot.robotName} (${robot.batteryLevel}%)", rx + 28f, ry + 8f, textPaint)
        }
    }
}
