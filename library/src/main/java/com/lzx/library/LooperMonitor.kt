package com.lzx.library

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.MessageQueue
import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import me.weishu.reflection.Reflection
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue

object LooperMonitor {

    private var mH: Handler? = null
    private var mHMsgWhat = 0
    private val mSessionPool = ConcurrentLinkedQueue<DispatchSession>()
    private val mEntries = SparseArray<Entry>(512)
    val entryList = mutableListOf<Entry>()
    private var mSamplingInterval = 1000 //采样间隔
    private const val SESSION_POOL_SIZE = 500
    private const val mEntriesSizeCap = 1500 //msg类型最大量
    private val mLock = Any()
    var formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private val mOverflowEntry = Entry("OVERFLOW")
    private val mHashCollisionEntry = Entry("HASH_COLLISION")
    var callback: EntryCallback? = null

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= 28) {
            Reflection.unseal(context.applicationContext)
        }
        hookActivityThreadHandler(context)
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun getActivityThreadHandler(): Handler {
        val activityThread = Class.forName("android.app.ActivityThread")
        val sCurrentActivityThreadF = activityThread.getDeclaredField("sCurrentActivityThread")
        sCurrentActivityThreadF.isAccessible = true
        val sCurrentActivityThread = sCurrentActivityThreadF.get(null)

        val mHF = activityThread.getDeclaredField("mH")
        mHF.isAccessible = true
        val mH = mHF.get(sCurrentActivityThread)
        return mH as Handler
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun hookActivityThreadHandler(context: Context) {
        if (mH == null) {
            mH = getActivityThreadHandler()
        }
        if (mH == null) return
        if (Build.VERSION.SDK_INT >= 28) {
            hookLooperObserver(context)
        } else {
            setLooperMessageLogging()
        }
        val callback = Handler::class.java.getDeclaredField("mCallback")
        callback.isAccessible = true
        callback.set(mH, object : Handler.Callback {
            override fun handleMessage(msg: Message): Boolean {
                mHMsgWhat = msg.what
                return false
            }
        })
    }

    @SuppressLint("PrivateApi")
    private fun hookLooperObserver(context: Context) {
        val observer = Class.forName("android.os.Looper\$Observer")
        val invocationHandler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "messageDispatchStarting" -> {
                    if (shouldCollectDetailedData()) {
                        var session = mSessionPool.poll()
                        session = session ?: DispatchSession()
                        session.startTimeMicro = getElapsedRealtimeMicro()
                        session.systemUptimeMillis = getSystemUptimeMillis()
                        return@InvocationHandler session
                    }
                    return@InvocationHandler DispatchSession.NOT_SAMPLED
                }
                "messageDispatched" -> {
                    if (shouldCollectDetailedData()) {
                        val session = args[0] as DispatchSession
                        val msg = args[1] as Message
                        val entry = findEntry(msg, session !== DispatchSession.NOT_SAMPLED)
                        if (entry != null) {
                            synchronized(entry) {
                                entry.messageCount++
                                entry.currTime = formatter.format(System.currentTimeMillis())
                                if (session !== DispatchSession.NOT_SAMPLED) {
                                    entry.recordedMessageCount++
                                    val latency = getElapsedRealtimeMicro() - session.startTimeMicro
                                    entry.latencyMicro = latency
                                    entry.totalLatencyMicro += latency
                                    entry.maxLatencyMicro = entry.maxLatencyMicro.coerceAtLeast(latency)
                                    if (msg.getWhen() > 0) {
                                        val delay = 0L.coerceAtLeast(session.systemUptimeMillis - msg.getWhen())
                                        entry.delayMillis += delay
                                        entry.maxDelayMillis = entry.maxDelayMillis.coerceAtLeast(delay)
                                        entry.recordedDelayMessageCount++
                                    }
                                    entry.mHMsgWhat = Entry.codeToString(mHMsgWhat)
                                    if (entry.msgTarget?.contains("ActivityThread") == true) {
                                        entry.msgType = MsgType.SystemMsg
                                    } else if (entry.latencyMicro < 30) {
                                        entry.msgType = MsgType.ClusterMsg
                                    } else if (entry.latencyMicro in 30..200) {
                                        entry.msgType = MsgType.RollingMsg
                                    } else if (entry.latencyMicro > 200) {
                                        entry.msgType = MsgType.FatMsg
                                    }
                                }
                                if (entry.latencyMicro > 30) {
                                    Log.i("BBBB", "name = " + entry.msgType?.name)
                                    callback?.onEntry(entry)
                                }
                            }
                        } else {
                            //NOTHING
                        }
                        recycleSession(session)
                    } else {
                        //NOTHING
                    }
                }
                "dispatchingThrewException" -> {
                    if (shouldCollectDetailedData()) {
                        val session = args[0] as DispatchSession
                        val msg = args[1] as Message
                        val entry = findEntry(msg, session !== DispatchSession.NOT_SAMPLED)
                        if (entry != null) {
                            synchronized(entry) {
                                entry.exceptionCount++
                                entry.currTime = formatter.format(System.currentTimeMillis())
                                callback?.onEntry(entry)
                            }
                        } else {
                            //NOTHING
                        }
                        recycleSession(session)
                    } else {
                        //NOTHING
                    }
                }
                else -> {
                }
            }
        }
        val proxy = Proxy.newProxyInstance(context.classLoader, arrayOf(observer), invocationHandler)
        val looper = Looper.getMainLooper().javaClass.getMethod("setObserver", observer)
        looper.invoke(null, proxy)
    }

    private fun setLooperMessageLogging() {
        var msgTarget: String? = null
        var msgCallback: String? = null
        var msgWhat: String? = null
        var session: DispatchSession? = DispatchSession.NOT_SAMPLED
        Looper.getMainLooper().setMessageLogging { log ->
            if (!shouldCollectDetailedData()) {
                return@setMessageLogging
            }
            if (log.startsWith(">>>>> Dispatching to ")) {
                val msgInfo = log.replace(">>>>> Dispatching to ", "")
                msgTarget = msgInfo.substring(0, msgInfo.indexOf("} ") + 1)
                msgCallback = msgInfo.substring(msgInfo.indexOf("} ") + 2, msgInfo.lastIndexOf(": "))
                msgWhat = msgInfo.substring(msgInfo.indexOf(": ") + 2, msgInfo.length)
                session = mSessionPool.poll()
                session = session ?: DispatchSession()
                session?.startTimeMicro = getElapsedRealtimeMicro()
                session?.systemUptimeMillis = getSystemUptimeMillis()
            } else {
                if (session != null) {
                    val entry = findEntry(
                        target = msgTarget,
                        callback = msgCallback,
                        what = msgWhat,
                        allowCreateNew = session !== DispatchSession.NOT_SAMPLED
                    )
                    if (entry != null) {
                        synchronized(entry) {
                            entry.currTime = formatter.format(System.currentTimeMillis())
                            entry.messageCount++
                            if (session !== DispatchSession.NOT_SAMPLED) {
                                entry.recordedMessageCount++
                                val latency = getElapsedRealtimeMicro() - session!!.startTimeMicro
                                entry.latencyMicro = latency
                                entry.totalLatencyMicro += latency
                                entry.maxLatencyMicro = entry.maxLatencyMicro.coerceAtLeast(latency)
                                entry.mHMsgWhat = Entry.codeToString(mHMsgWhat)
                                if (entry.msgTarget?.contains("ActivityThread") == true) {
                                    entry.msgType = MsgType.SystemMsg
                                } else if (entry.latencyMicro < 30) {
                                    entry.msgType = MsgType.ClusterMsg
                                } else if (entry.latencyMicro in 30..200) {
                                    entry.msgType = MsgType.RollingMsg
                                } else if (entry.latencyMicro > 200) {
                                    entry.msgType = MsgType.FatMsg
                                }
                            }
                            if (entry.latencyMicro > 30) {
                                callback?.onEntry(entry)
                            }
                        }
                    }
                    recycleSession(session!!)
                }
            }
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun hookMessageQueue() {
        if (mH == null) return
        val queueF = Handler::class.java.getDeclaredField("mQueue")
        queueF.isAccessible = true
        val queue = queueF.get(mH) as MessageQueue
    }

    private fun findEntry(msg: Message, allowCreateNew: Boolean): Entry? {
        val id: Int = Entry.idFor(msg)
        var entry: Entry?
        synchronized(mLock) {
            entry = mEntries.get(id)
            if (entry == null) {
                if (!allowCreateNew) {
                    return null
                } else if (mEntries.size() >= mEntriesSizeCap) {
                    return mOverflowEntry  //如果超过 mEntriesSizeCap 种 msg，则覆盖
                } else {
                    entry = Entry(msg)
                    mEntries.put(id, entry)
                }
            }
        }
        //发生hash冲突
        if (entry?.handlerClassName != msg.target.javaClass.name
            || entry?.threadName !== msg.target.looper.thread.name
        ) {
            return mHashCollisionEntry
        }
        return entry
    }

    private fun findEntry(target: String?, callback: String?, what: String?, allowCreateNew: Boolean): Entry? {
        if (target.isNullOrEmpty() || what.isNullOrEmpty()) {
            return null
        }
        val id: Int = Entry.idFor(target, callback, what)
        var entry: Entry?
        synchronized(mLock) {
            entry = mEntries.get(id)
            if (entry == null) {
                if (!allowCreateNew) {
                    return null
                } else if (mEntries.size() >= mEntriesSizeCap) {
                    return mOverflowEntry  //如果超过 mEntriesSizeCap 种 msg，则覆盖
                } else {
                    entry = Entry(target, callback, what)
                    mEntries.put(id, entry)
                }
            }
        }
        //发生hash冲突
        if (entry?.msgTarget != target
            || entry?.messageName != callback
            || entry?.msgWhat != what
        ) {
            return mHashCollisionEntry
        }
        return entry
    }

    private fun recycleSession(session: DispatchSession) {
        if (session !== DispatchSession.NOT_SAMPLED) {
            if (mSessionPool.size < SESSION_POOL_SIZE) {
                mSessionPool.add(session)
            }
        }
    }

    /**
     * 获取数据
     */
    fun getEntries(): MutableList<Entry> {
        val exportedEntries: ArrayList<Entry>
        synchronized(mLock) {
            val size = mEntries.size()
            exportedEntries = ArrayList(size)
            for (i in 0 until size) {
                val entry: Entry = mEntries.valueAt(i)
                synchronized(entry) { exportedEntries.add(entry) }
            }
        }
        maybeAddSpecialEntry(exportedEntries, mOverflowEntry)
        maybeAddSpecialEntry(exportedEntries, mHashCollisionEntry)
        return exportedEntries
    }

    fun getMsgList(): MutableList<Entry> {
        val exportedEntries: MutableList<Entry>
        synchronized(mLock) {
            val size = entryList.size
            exportedEntries = mutableListOf()
            for (i in 0 until size) {
                val entry: Entry = entryList[i]
                synchronized(entry) { exportedEntries.add(entry) }
            }
        }
        return exportedEntries
    }

    private fun maybeAddSpecialEntry(
        exportedEntries: MutableList<Entry>,
        specialEntry: Entry
    ) {
        synchronized(specialEntry) {
            if (specialEntry.messageCount > 0 || specialEntry.exceptionCount > 0) {
                exportedEntries.add(specialEntry)
            }
        }
    }

    /**
     * 判断是否要采样该条Msg
     */
    private fun shouldCollectDetailedData(): Boolean {
        return true
//        return ThreadLocalRandom.current().nextInt() % mSamplingInterval == 0
    }

    /**
     * 开机到现在包括系统深度睡眠的微秒
     */
    private fun getElapsedRealtimeMicro(): Long {
        return SystemClock.elapsedRealtimeNanos() / 1000000
    }

    /**
     * 开机到现在不包括深度睡眠的毫秒
     */
    private fun getSystemUptimeMillis(): Long {
        return SystemClock.uptimeMillis()
    }
}

interface EntryCallback {
    fun onEntry(entry: Entry)
}