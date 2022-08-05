package com.lzx.loopermonitor

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    private var recyclerView: CoordinateView? = null
    private var detailView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recycleView)
        detailView = findViewById(R.id.detailInfo)

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


    private fun initRecyclerView() {
        recyclerView?.linear()?.setup {
            addType<Entry>(R.layout.item_monitor)
            onBind {
                val info = getModel<Entry>()
                var f = info.latencyMicro.toFloat() / 1000f
                if (f > 1) {
                    f = 1f
                }
                val length = f * recyclerView!!.measuredWidth.toFloat()
                val item = findView<View>(R.id.monitorItem)
                item.layoutParams.width = length.toInt()
                setMargins(item, dp(1f), if (modelPosition == 0) dp(25f) else dp(5f), 0, 0, false)
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
                R.id.titleName.setText(getModel<DetailInfo>().title+":  ")
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
