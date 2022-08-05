package com.lzx.loopermonitor

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.lze.loopermonitor.R
import com.lzx.library.Entry
import com.lzx.library.EntryCallback
import com.lzx.library.LooperMonitor
import com.lzx.library.MsgType
import com.lzx.loopermonitor.adapter.linear
import com.lzx.loopermonitor.adapter.models
import com.lzx.loopermonitor.adapter.notifyItemInserted
import com.lzx.loopermonitor.adapter.setup
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var recyclerView: RecyclerView? = null
    private var detailView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recycleView)
        detailView = findViewById(R.id.detailInfo)

        setBackgroundUI()
        initRecyclerView()
        initDetailInfo()

        findViewById<Button>(R.id.btn).setOnClickListener {
            Thread.sleep(5000)
        }
        findViewById<Button>(R.id.start).setOnClickListener {

        }

        lifecycleScope.launch {
            LooperMonitor.entryFlow()
                .collectLatest { entry ->
                    recyclerView?.notifyItemInserted(0, Entry().copy(entry))
                    recyclerView?.scrollToPosition(0)
                }
        }
    }

    private fun LooperMonitor.entryFlow() = callbackFlow<Entry> {
        callback = object : EntryCallback {
            override fun onEntry(entry: Entry) {
                offer(entry)
            }
        }
        awaitClose { callback = null }
    }

    private fun setBackgroundUI() {
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
    }

    private fun initRecyclerView() {
        val maxWidth = Resources.getSystem().displayMetrics.widthPixels - dp(60f)
        recyclerView?.linear()?.setup {
            addType<Entry>(R.layout.item_monitor)
            onBind {
                val info = getModel<Entry>()
                Log.i("AAA", "info=" + info.latencyMicro)
                var f = info.latencyMicro.toFloat() / 550f
                if (f > 1) {
                    f = 1f
                }
                val length = f * maxWidth
                val item = findView<View>(R.id.monitorItem)
                item.layoutParams.width = length.toInt()
                item.tag = info
                when (info.msgType) {
                    MsgType.ClusterMsg -> {
                        item.setBackgroundColor(Color.parseColor("#807C7C"))
                    }
                    MsgType.RollingMsg -> {
                        item.setBackgroundColor(Color.parseColor("#96c485"))
                    }
                    MsgType.FatMsg -> {
                        item.setBackgroundColor(Color.parseColor("#e15436"))
                    }
                    MsgType.SystemMsg -> {
                        item.setBackgroundColor(Color.parseColor("#78b3f3"))
                    }
                    else -> {
                        item.setBackgroundColor(Color.parseColor("#807C7C"))
                    }
                }
                item.setOnClickListener {
                    val list = mutableListOf<DetailInfo>()
                    list.add(DetailInfo("消息类型", info.msgType?.name.toString()))
                    list.add(DetailInfo("记录时间", info.currTime.toString()))
                    list.add(DetailInfo("记录次数", info.recordedMessageCount.toString()))
                    list.add(DetailInfo("What", info.msgWhat.toString()))
                    list.add(DetailInfo("mH what", info.mHMsgWhat.toString()))
                    list.add(DetailInfo("消息耗时", info.latencyMicro.toString()))
                    list.add(DetailInfo("Handler", info.handlerClassName.toString()))
                    list.add(DetailInfo("Callback", info.messageName.toString()))
                    list.add(DetailInfo("stack", info.stack.toString()))
                    detailView?.models = list
                }
            }
        }?.models = mutableListOf<Entry>()
    }

    private fun initDetailInfo() {
        detailView?.linear()?.setup {
            addType<DetailInfo>(R.layout.item_detail)
            onBind {
                R.id.titleName.setText(getModel<DetailInfo>().title)
                R.id.detailInfo.setText(getModel<DetailInfo>().detail)
            }
        }?.models = mutableListOf<DetailInfo>()
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

fun dp(dp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        Resources.getSystem().displayMetrics
    ).toInt()
}

data class DetailInfo(var title: String, var detail: String)
