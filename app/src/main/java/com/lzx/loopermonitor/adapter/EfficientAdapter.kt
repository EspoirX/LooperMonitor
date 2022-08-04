package com.lzx.loopermonitor.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.NoSuchPropertyException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Modifier

@Suppress("UNCHECKED_CAST")
open class EfficientAdapter : RecyclerView.Adapter<EfficientAdapter.EfficientViewHolder>() {

    /** 当前Adapter被setAdapter才不为null */
    var rv: RecyclerView? = null

    /** 生命周期 */
    private var onCreate: (EfficientViewHolder.(viewType: Int) -> Unit)? = null
    private var onBind: (EfficientViewHolder.() -> Unit)? = null
    private var onPayload: (EfficientViewHolder.(model: Any) -> Unit)? = null
    private var onClick: (EfficientViewHolder.(viewId: Int) -> Unit)? = null
    private var onLongClick: (EfficientViewHolder.(viewId: Int) -> Unit)? = null

    /**
     * [onCreateViewHolder]执行时回调
     */
    fun onCreate(block: EfficientViewHolder.(viewType: Int) -> Unit) {
        onCreate = block
    }

    /**
     * [onBindViewHolder]执行时回调
     */
    fun onBind(block: EfficientViewHolder.() -> Unit) {
        onBind = block
    }

    /**
     * 增量更新回调
     * 当你使用[notifyItemChanged(int, Object)]或者[notifyItemRangeChanged(int, Object)]等方法更新列表时才会触发,
     * 并且形参payload要求不能为null
     *
     * @param block 形参model即为[notifyItemChanged]中的形参payload
     */
    fun onPayload(block: EfficientViewHolder.(model: Any) -> Unit) {
        onPayload = block
    }

    private var context: Context? = null
    private var lastPosition = -1
    private var isFirst = true

    /** 数据模型数量  */
    val modelCount: Int
        get() {
            return if (models == null) 0 else models!!.size
        }

    /** 原始的数据集合对象, 不会经过任何处理 */
    var _data: List<Any?>? = null

    /**
     * 数据模型集合
     * 如果赋值的是[List]不可变集合将会自动被替换成[MutableList], 将无法保持为同一个集合对象引用
     */
    var models: List<Any?>?
        get() = _data
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            _data = when (value) {
                is ArrayList -> flat(value)
                is List -> flat(value.toMutableList())
                else -> null
            }
            notifyDataSetChanged()
            if (isFirst) {
                lastPosition = -1
                isFirst = false
            } else {
                lastPosition = itemCount - 1
            }
        }

    /** 可增删的数据模型集合, 本质上就是返回可变的models. 假设未赋值给models则将抛出异常为[ClassCastException] */
    var mutable
        get() = models as ArrayList
        set(value) {
            models = value
        }

    /** 对比新旧数据更改列表接口 */
    var itemDifferCallback: ItemDifferCallback = ItemDifferCallback

    /**
     * 对比数据, 根据数据差异自动刷新列表
     * 数据对比默认使用`equals`函数对比, 你可以为数据手动实现equals函数来修改对比逻辑.
     * 推荐定义数据为 data class, 因其会根据构造参数自动生成equals
     * 如果数据集合很大导致对比速度很慢, 建议在非主步线程中调用此函数, 效果等同于[androidx.recyclerview.widget.AsyncListDiffer]
     *
     * 对于数据是否匹配可能需要你自定义[itemDifferCallback], 因为默认使用数据模型的[equals]方法匹配,
     * 具体请阅读[ItemDifferCallback.DEFAULT]
     *
     * @param newModels 新的数据, 将覆盖旧的数据
     * @param detectMoves 是否对比Item的移动, true会导致列表当前位置发生移动
     * @param commitCallback 因为子线程调用[setDifferModels]刷新列表会不同步(刷新列表需要切换到主线程),
     * 而[commitCallback]保证在刷新列表完成以后调用(运行在主线程)
     */
    fun setDifferModels(
        newModels: List<Any?>?,
        detectMoves: Boolean = true,
        commitCallback: Runnable? = null
    ) {
        val oldModels = _data
        _data = newModels
        val diffResult =
            DiffUtil.calculateDiff(ProxyDiffCallback(newModels, oldModels, itemDifferCallback), detectMoves)
        val mainLooper = Looper.getMainLooper()
        if (Looper.myLooper() != mainLooper) {
            Handler(mainLooper).post {
                diffResult.dispatchUpdatesTo(this)
                commitCallback?.run()
            }
        } else {
            diffResult.dispatchUpdatesTo(this)
            commitCallback?.run()
        }
    }

    /**
     * 针对单类型的时候可用，不用强转，多类型bean不能用
     */
    inline fun <reified M> getMutableList(): MutableList<M> {
        return mutable.mapNotNull { if (it is M) it else null } as MutableList<M>
    }

    /**
     * 根据索引返回数据模型, 如果不存在该模型则返回Null
     */
    inline fun <reified M> getModelOrNull(position: Int): M? {
        return models?.let { it.getOrNull(position) as? M }
    }

    /**
     * 根据索引返回数据模型, 不存在该模型则抛出异常
     */
    fun <M> getModel(@IntRange(from = 0) position: Int): M {
        return models!!.let { it[position] as M }
    }

    /**
     * 添加新的数据
     * @param models 被添加的数据
     * @param animation 是否使用动画
     * @param index 插入到[models]指定位置, 如果index超过[models]长度则会添加到最后
     */
    @SuppressLint("NotifyDataSetChanged")
    fun addModels(
        models: List<Any?>?,
        animation: Boolean = true,
        @IntRange(from = -1) index: Int = -1
    ) {
        if (models.isNullOrEmpty()) return
        val data: MutableList<Any?> = when (models) {
            is ArrayList -> models
            else -> models.toMutableList()
        }
        when {
            this.models == null -> {
                this.models = flat(data)
                notifyDataSetChanged()
            }
            this.models?.isEmpty() == true -> {
                (this.models as? MutableList)?.let {
                    it.addAll(flat(data))
                    notifyDataSetChanged()
                }
            }
            else -> {
                val realModels = this.models as MutableList
                var insertIndex = 0
                if (index == -1 || realModels.size < index) {
                    insertIndex += realModels.size
                    realModels.addAll(flat(data))
                } else {
                    insertIndex += index
                    realModels.addAll(index, flat(data))
                }
                if (animation) {
                    notifyItemRangeInserted(insertIndex, data.size)
                    rv?.post {
                        rv?.invalidateItemDecorations()
                    }
                } else {
                    notifyDataSetChanged()
                }
            }
        }
    }

    /** 类型池 */
    val typePool = mutableMapOf<Class<*>, Any.(Int) -> Int>()
    var interfacePool: MutableMap<Class<*>, Any.(Int) -> Int>? = null

    /**
     * 添加多类型
     * 在BRV中一个Item类型就是对应一个唯一的布局文件Id, 而[M]即为对应该类型所需的数据类型.
     * 只要使用该函数添加的元素类型才被允许赋值给[models].
     */
    inline fun <reified M> addType(@LayoutRes layout: Int) {
        if (Modifier.isInterface(M::class.java.modifiers)) {
            M::class.java.addInterfaceType { layout }
        } else {
            typePool[M::class.java] = { layout }
        }
    }

    /**
     * 通过回调函数添加多类型, 一对多多类型(即一个数据类对应多个布局)
     * [block]中的position为当前item位于列表中的索引, [M]则为rv的models中对应的数据类型
     */
    inline fun <reified M> addType(noinline block: M.(position: Int) -> Int) {
        if (Modifier.isInterface(M::class.java.modifiers)) {
            M::class.java.addInterfaceType(block as Any.(Int) -> Int)
        } else {
            typePool[M::class.java] = block as Any.(Int) -> Int
        }
    }

    /**
     * 接口类型, 即类型必须为接口, 同时其子类都会被认为属于该接口而对应其布局
     * @receiver 接口类
     * @see addType
     */
    fun Class<*>.addInterfaceType(block: Any.(Int) -> Int) {
        (interfacePool ?: mutableMapOf<Class<*>, Any.(Int) -> Int>().also {
            interfacePool = it
        })[this] = block
    }

    private val clickListeners = HashMap<Int, (EfficientViewHolder.(Int) -> Unit)?>()
    private val longClickListeners = HashMap<Int, (EfficientViewHolder.(Int) -> Unit)?>()

    /**
     * 监听指定Id控件的点击事件, 包含防抖动
     */
    fun onClick(@IdRes vararg id: Int, block: EfficientViewHolder.(viewId: Int) -> Unit) {
        for (i in id) {
            clickListeners[i] = block
        }
        onClick = block
    }

    /**
     * 长按点击事件回调
     */
    fun onLongClick(@IdRes vararg id: Int, block: EfficientViewHolder.(viewId: Int) -> Unit) {
        for (i in id) {
            longClickListeners[i] = block
        }
        onLongClick = block
    }

    /**
     * 添加点击事件
     */
    fun @receiver:IdRes Int.onClick(listener: EfficientViewHolder.(viewId: Int) -> Unit) {
        clickListeners[this] = listener
    }

    /**
     * 添加长按事件
     */
    fun @receiver:IdRes Int.onLongClick(listener: EfficientViewHolder.(viewId: Int) -> Unit) {
        longClickListeners[this] = listener
    }

    /**
     * 这里已viewType作为布局id，即每个类型对应一个唯一的布局
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EfficientViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        val vh = EfficientViewHolder(view)
        onCreate?.invoke(vh, viewType)
        return vh
    }

    override fun onBindViewHolder(holder: EfficientViewHolder, position: Int) {
        holder.bind(getModel(position))
    }

    override fun onBindViewHolder(holder: EfficientViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && onPayload != null) {
            onPayload?.invoke(holder, payloads[0])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val model = getModel<Any>(position)
        val modelClass: Class<*> = model.javaClass
        return (typePool[modelClass]?.invoke(model, position) ?: interfacePool?.run {
            for (interfaceType in this) {
                if (interfaceType.key.isAssignableFrom(modelClass)) {
                    return@run interfaceType.value.invoke(model, position)
                }
            }
            null
        }
        ?: throw NoSuchPropertyException(
            "please add item model type : " +
                "addType<${model.javaClass.name}>(R.layout.item)"
        ))
    }

    override fun getItemCount(): Int = modelCount

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.rv = recyclerView
        if (context == null) {
            context = recyclerView.context
        }
    }


    /**
     * 扁平化数据, 将折叠分组铺平展开创建列表
     * @param models 数据集合
     */
    private fun flat(
        models: MutableList<Any?>
    ): MutableList<Any?> {
        if (models.isEmpty()) return models
        val arrayList = ArrayList(models)
        models.clear()
        arrayList.forEachIndexed { index, item ->
            models.add(item)
        }
        return models
    }

    /**
     * 更新数据
     */
    fun notifyItemChanged(position: Int, data: Any?, payload: Boolean = false) {
        runCatching {
            if (position >= 0 && position <= mutable.lastIndex) {
                mutable[position] = data
                if (payload) {
                    notifyItemChanged(position, data)
                } else {
                    notifyItemChanged(position)
                }
            }
        }
    }

    inner class EfficientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        lateinit var _data: Any private set
        var context: Context = this@EfficientAdapter.context!!
        val adapter: EfficientAdapter = this@EfficientAdapter
        val modelPosition get() = layoutPosition

        init {
            for (clickListener in clickListeners) {
                val view = itemView.findViewById<View>(clickListener.key) ?: continue
                view.setOnClickListener {
                    (clickListener.value ?: onClick)?.invoke(this, it.id)
                }
            }
            for (longClickListener in longClickListeners) {
                val view = itemView.findViewById<View>(longClickListener.key) ?: continue
                view.setOnLongClickListener {
                    (longClickListener.value ?: onLongClick)?.invoke(this, it.id)
                    true
                }
            }
        }

        internal fun bind(model: Any) {
            this._data = model
            onBind?.invoke(this@EfficientViewHolder)
        }

        /**
         * 查找ItemView上的视图
         */
        fun <V : View?> findView(@IdRes id: Int): V = itemView.findViewById<V>(id)

        /**
         * 返回数据模型
         */
        fun <M> getModel(): M = _data as M

        /**
         * 返回数据模型, 如果不匹配泛型则返回Null
         */
        inline fun <reified M> getModelOrNull(): M? = _data as? M


        fun @receiver:IdRes Int.setText(text: CharSequence?) {
            findView<TextView>(this).text = text
        }

        fun @receiver:IdRes Int.setImageResource(@DrawableRes resId: Int) {
            findView<ImageView>(this).setImageResource(resId)
        }

        fun @receiver:IdRes Int.setBackgroundResource(@DrawableRes resId: Int) {
            findView<View>(this).setBackgroundResource(resId)
        }

        fun @receiver:IdRes Int.setTextColor(@ColorInt color: Int) {
            findView<TextView>(this).setTextColor(color)
        }

        fun @receiver:IdRes Int.visible() {
            findView<View>(this).visibility = View.VISIBLE
        }

        fun @receiver:IdRes Int.gone() {
            findView<View>(this).visibility = View.GONE
        }

        fun @receiver:IdRes Int.visibilityBy(isVisible: Boolean) {
            findView<View>(this).let {
                if (isVisible) {
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
            }
        }


        fun EfficientViewHolder.itemClicked(block: View.OnClickListener.(v: View?) -> Unit) {
            itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    block.invoke(this, v)
                }
            })
        }

        fun @receiver:IdRes Int.onClick(block: View.OnClickListener.(v: View?) -> Unit) {
            findView<View>(this).setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    block.invoke(this, v)
                }
            })
        }
    }
}

