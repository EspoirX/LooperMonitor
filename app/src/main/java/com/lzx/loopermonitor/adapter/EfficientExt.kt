package com.lzx.loopermonitor.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.IntRange
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

val RecyclerView.efficientAdapter
    get() = adapter as? EfficientAdapter
        ?: throw NullPointerException("RecyclerView without EfficientAdapter")

/**
 * 设置适配器
 */
fun RecyclerView.setup(block: EfficientAdapter.(RecyclerView) -> Unit): EfficientAdapter {
    val adapter = EfficientAdapter()
    adapter.block(this)
    this.adapter = adapter
    return adapter
}

/**
 * 创建线性列表
 * @param orientation 列表方向
 * @param reverseLayout 是否反转列表
 */
fun RecyclerView.linear(
    @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    stackFromEnd: Boolean = false,
    recycleChildrenOnDetach: Boolean = false
): RecyclerView {
    layoutManager = LinearLayoutManager(context, orientation, reverseLayout).apply {
        this.stackFromEnd = stackFromEnd
        this.recycleChildrenOnDetach = recycleChildrenOnDetach
    }
    return this
}

/**
 * 创建网格列表
 * @param spanCount 网格跨度数量
 * @param orientation 列表方向
 * @param reverseLayout 是否反转
 */
fun RecyclerView.grid(
    spanCount: Int = 1,
    @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
): RecyclerView {
    layoutManager = GridLayoutManager(context, spanCount, orientation, reverseLayout)
    return this
}

/**
 * 对比数据, 根据数据差异自动刷新列表
 */
fun RecyclerView.setDifferModels(
    newModels: List<Any?>?,
    detectMoves: Boolean = true,
    commitCallback: Runnable? = null
) {
    efficientAdapter.setDifferModels(newModels, detectMoves, commitCallback)
}

/**
 * 数据模型集合
 * 如果赋值的是[List]不可变集合将会自动被替换成[MutableList], 将无法保持为同一个集合对象引用
 */
var RecyclerView.models
    get() = efficientAdapter.models
    set(value) {
        efficientAdapter.models = value
    }

fun RecyclerView.setModels(models: List<Any?>?, refresh: Boolean) {
    if (models.isNullOrEmpty()) return
    if (refresh) {
        this.models = models
    } else {
        val list = this.mutable
        list?.addAll(models.toMutableList())
        this.models = list?.distinct()
    }
}

/**
 * 可增删的数据模型集合, 本质上就是返回可变的models
 */
var RecyclerView.mutable
    get() = efficientAdapter.models as? ArrayList?
    set(value) {
        efficientAdapter.models = value
    }

/**
 * 针对单类型的时候可用，不用强转，多类型bean不能用
 */
inline fun <reified M> RecyclerView.getMutableList(): MutableList<M> {
    return efficientAdapter.getMutableList()
}

/**
 * 添加新的数据
 * @param models 被添加的数据
 * @param animation 是否使用动画
 * @param index 插入到[models]指定位置, 如果index超过[models]长度则会添加到最后
 */
fun RecyclerView.addModels(
    models: List<Any?>?,
    animation: Boolean = true,
    @IntRange(from = -1) index: Int = -1
) {
    efficientAdapter.addModels(models, animation, index)
}

/**
 * 更新数据
 */
fun RecyclerView.notifyItemChanged(position: Int, data: Any?, payload: Boolean = false) {
    efficientAdapter.notifyItemChanged(position, data, payload)
}

fun RecyclerView.notifyItemInserted(position: Int, data: Any?) {
    this.mutable?.add(position, data)
    efficientAdapter.notifyDataSetChanged()
//    efficientAdapter.notifyItemInserted(position)
}

/**
 * 创建 RecyclerView
 */
fun Context.createRecycler(): RecyclerView {
    return RecyclerView(this)
}

fun RecyclerView.matchParams(): RecyclerView {
    layoutParams = RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    return this
}

