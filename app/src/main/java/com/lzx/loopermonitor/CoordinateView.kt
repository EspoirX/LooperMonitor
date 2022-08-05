package com.lzx.loopermonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CoordinateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var paintAxes: Paint? = null
    private var paintAxes2: Paint? = null

    init {

        paintAxes = Paint()
        paintAxes?.style = Paint.Style.STROKE
        paintAxes?.isAntiAlias = true
        paintAxes?.isDither = true
        paintAxes?.color = Color.parseColor("#e15436")
        paintAxes?.strokeWidth = 1f

        paintAxes2 = Paint()
        paintAxes2?.style = Paint.Style.FILL

        paintAxes2?.color = Color.WHITE
        paintAxes2?.textSize = 28f
    }

    private val xNums = arrayOf("0", "100", "200", "300", "400", "500", "600", "700", "800", "900", "1000")
    private val lineNum = 10
    private val maxX = 1000f
    private val maxY = 1000f
    private val startX = 0f
    private val startY = 40f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        //画表格
        for (i in 1..lineNum) {
            canvas?.drawLine(startX, startY, maxX, startY, paintAxes2!!)
            canvas?.drawLine(startX, (maxY / lineNum * i) + startY, maxX, (maxY / lineNum * i) + startY, paintAxes2!!)
            canvas?.drawLine(startX, maxY + startY, startX, startX + startY, paintAxes2!!)
            canvas?.drawLine(
                (maxX - startX) / lineNum * i, maxY + startY,
                (maxX - startX) / lineNum * i, startX + startY, paintAxes2!!
            )
        }
        //话刻度
        for (i in 1..lineNum) {
            val length = paintAxes2?.measureText(xNums[i]) ?: 0f
            canvas?.drawText(xNums[i], (maxX / lineNum * i) - length / 2, 30f, paintAxes2!!);
        }
    }
}