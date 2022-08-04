package com.lzx.loopermonitor

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.lze.loopermonitor.R
import com.lzx.library.Entry
import com.lzx.library.EntryCallback
import com.lzx.library.LooperMonitor
import com.lzx.library.MsgType
import com.lzx.loopermonitor.adapter.efficientAdapter
import com.lzx.loopermonitor.adapter.linear
import com.lzx.loopermonitor.adapter.mutable
import com.lzx.loopermonitor.adapter.notifyItemInserted
import com.lzx.loopermonitor.adapter.setup

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.btn).setOnClickListener {
            Thread.sleep(5000)
        }

        val maxWidth = Resources.getSystem().displayMetrics.widthPixels - dp(60f)

        val tabNumLayout = findViewById<LinearLayout>(R.id.tabNumLayout)
        val tabLineLayout = findViewById<LinearLayout>(R.id.tabLineLayout)
        val paint = Paint()
        paint.textSize = 14f
        tabNumLayout.post {
            val leftM = dp(15f) - paint.measureText("0") / 2
            setMargins(tabNumLayout, leftM.toInt(), 0, 0, 0, false)
            val layoutWidth = tabNumLayout.measuredWidth
            val itemWidth = layoutWidth.toFloat() / 10f
            for (i in 0..5) {
                val num = TextView(this)
                val text = (i * 100).toString()
                val length = paint.measureText(text)
                num.text = text
                num.textSize = 14f
                num.setTextColor(Color.WHITE)
                num.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val left = itemWidth * i
                val translationX = left - length / 2
                num.translationX = if (translationX < 0) 0f else translationX
                tabNumLayout.addView(num)

                val line = View(this)
                line.layoutParams = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT)
                line.setBackgroundColor(if (i == 2) Color.parseColor("#e15436") else Color.WHITE)
                when (i) {
                    0 -> line.translationX = 0f
                    1 -> line.translationX = 141f
                    2 -> line.translationX = 314f
                    3 -> line.translationX = 480f
                    4 -> line.translationX = 650f
                    5 -> line.translationX = 828f
                }
                tabLineLayout.addView(line)
            }
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycleView)
        val adapter = MonitorAdapter()
        recyclerView.linear().adapter = adapter
//        recyclerView.linear().setup {
//            addType<Entry>(R.layout.item_monitor)
//            onBind {
//                val info = getModel<Entry>()
//                var f = info.latencyMicro.toFloat() / 550f
//                if (f > 1) {
//                    f = 1f
//                }
//                val length = f * maxWidth
//                val item = findView<View>(R.id.monitorItem)
//                item.layoutParams.width = length.toInt()
//                item.tag = info
//                when (info.msgType) {
//                    MsgType.ClusterMsg -> {
//                        item.setBackgroundColor(Color.parseColor("#807C7C"))
//                    }
//                    MsgType.RollingMsg -> {
//                        item.setBackgroundColor(Color.parseColor("#96c485"))
//                    }
//                    MsgType.FatMsg -> {
//                        item.setBackgroundColor(Color.parseColor("#e15436"))
//                    }
//                    MsgType.SystemMsg -> {
//                        item.setBackgroundColor(Color.parseColor("#78b3f3"))
//                    }
//                    else -> {
//                        item.setBackgroundColor(Color.parseColor("#807C7C"))
//                    }
//                }
//                item.setOnClickListener {
//                    val data = it.tag as Entry
//                    Toast.makeText(this@MainActivity, data.msgType?.name, Toast.LENGTH_SHORT).show()
//                }
//            }
//        }.models = mutableListOf<Entry>()

        LooperMonitor.callback = object : EntryCallback {
            override fun onEntry(entry: Entry) {
                runOnUiThread {
//                    recyclerView.notifyItemInserted(0, entry)
//                    recyclerView.scrollToPosition(0)
                    adapter.list.forEach {
                        Log.i("BBBB",it.msgType?.name)
                    }
                    Log.i("BBBB","----------------------")
                    adapter.list.add(0, entry)
                    adapter.notifyItemInserted(0)
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int, requestLayout: Boolean) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            if (requestLayout) {
                view.requestLayout()
            }
        }
    }


}


class MonitorAdapter : RecyclerView.Adapter<MonitorAdapter.MonitorHolder>() {

    val list = mutableListOf<Entry>()
    val maxWidth = Resources.getSystem().displayMetrics.widthPixels - dp(60f)

    class MonitorHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monitorItem: View

        init {
            monitorItem = itemView.findViewById(R.id.monitorItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_monitor, parent, false)
        return MonitorHolder(view)
    }

    override fun onBindViewHolder(holder: MonitorHolder, position: Int) {
        val info = list[position]
        var f = info.latencyMicro.toFloat() / 550f
        if (f > 1) {
            f = 1f
        }
        val length = f * maxWidth
        holder.monitorItem.layoutParams.width = length.toInt()
        holder.monitorItem.tag = info
        when (info.msgType) {
            MsgType.ClusterMsg -> {
                holder.monitorItem.setBackgroundColor(Color.parseColor("#807C7C"))
            }
            MsgType.RollingMsg -> {
                holder.monitorItem.setBackgroundColor(Color.parseColor("#96c485"))
            }
            MsgType.FatMsg -> {
                holder.monitorItem.setBackgroundColor(Color.parseColor("#e15436"))
            }
            MsgType.SystemMsg -> {
                holder.monitorItem.setBackgroundColor(Color.parseColor("#78b3f3"))
            }
            else -> {
                holder.monitorItem.setBackgroundColor(Color.parseColor("#807C7C"))
            }
        }
        holder.monitorItem.setOnClickListener {
            Toast.makeText(it.context, info.msgType?.name, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = list.size
}

fun dp(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        Resources.getSystem().displayMetrics
    ).toInt()
}
