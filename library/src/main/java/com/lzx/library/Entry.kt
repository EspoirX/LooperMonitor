package com.lzx.library

import android.os.Handler
import android.os.Message

class Entry {
    /**
     * msg的target handler，如果采用 setMessageLogging 方式，该值为 null，这时候看 msgTarget 字段即可
     */
    var handler: Handler? = null

    /**
     * 有callback的handler为callback类名 没有callback则为msg.what的十六进制表示(msg.callback)
     */
    var messageName: String? = null

    /**
     *  该msg被执行的次数，如果messageDispatchStarting 返回D ispatchSession.NOT_SAMPLED
     *  messageDispatched 会自加 messageCount，而不自加 recordedMessageCount
     */
    var messageCount: Long = 0

    /**
     * 该msg被执行且采样到的次数
     */
    var recordedMessageCount: Long = 0

    /**
     * 执行这个msg 抛异常的次数（回调dispatchingThrewException）
     */
    var exceptionCount: Long = 0

    /**
     * 执行这个msg总的消耗的时间
     */
    var totalLatencyMicro: Long = 0

    /**
     * 执行这个msg最大消耗的时间
     */
    var maxLatencyMicro: Long = 0

    /**
     * 执行这个Msg的耗时
     */
    var latencyMicro: Long = 0

    /**
     *  这个msg被延迟执行的次数
     */
    var recordedDelayMessageCount: Long = 0

    /**
     *  这个msg被延迟执行的累加毫秒数
     */
    var delayMillis: Long = 0

    /**
     * 这个msg被延迟执行的最大延迟时间
     */
    var maxDelayMillis: Long = 0

    /**
     * msg.target
     */
    var msgTarget: String? = null

    /**
     * msg.what
     */
    var msgWhat: String? = null

    /**
     * ActivityThread mH.what
     */
    var mHMsgWhat: String? = null

    fun reset() {
        messageCount = 0
        recordedMessageCount = 0
        exceptionCount = 0
        totalLatencyMicro = 0
        maxLatencyMicro = 0
        delayMillis = 0
        maxDelayMillis = 0
        recordedDelayMessageCount = 0
    }

    constructor(specialEntryName: String) {
        messageName = specialEntryName
        handler = null
    }

    constructor(msg: Message) {
        handler = msg.target
        messageName = handler?.getMessageName(msg)
        msgTarget = msg.target.toString()
        msgWhat = msg.what.toString()
    }

    constructor(target: String, callback: String?, what: String) {
        msgTarget = target
        messageName = callback.toString()
        msgWhat = what
    }

    companion object {
        fun idFor(msg: Message): Int {
            var result = 7
            result = 31 * result + msg.target.looper.thread.hashCode()
            result = 31 * result + msg.target.javaClass.hashCode()
            result = 31 * result + 1237
            return if (msg.callback != null) {
                31 * result + msg.callback.javaClass.hashCode()
            } else {
                31 * result + msg.what
            }
        }

        fun idFor(target: String, callback: String?, what: String): Int {
            var result = 7
            result = 31 * result + target.hashCode()
            result = 31 * result + 1237
            return if (callback != null) {
                31 * result + callback.hashCode()
            } else {
                31 * result + what.toInt()
            }
        }

        fun codeToString(code: Int): String {
            when (code) {
                110 -> return "BIND_APPLICATION"
                111 -> return "EXIT_APPLICATION"
                113 -> return "RECEIVER"
                114 -> return "CREATE_SERVICE"
                115 -> return "SERVICE_ARGS"
                116 -> return "STOP_SERVICE"
                118 -> return "CONFIGURATION_CHANGED"
                119 -> return "CLEAN_UP_CONTEXT"
                120 -> return "GC_WHEN_IDLE"
                121 -> return "BIND_SERVICE"
                122 -> return "UNBIND_SERVICE"
                123 -> return "DUMP_SERVICE"
                124 -> return "LOW_MEMORY"
                127 -> return "PROFILER_CONTROL"
                128 -> return "CREATE_BACKUP_AGENT"
                129 -> return "DESTROY_BACKUP_AGENT"
                130 -> return "SUICIDE"
                131 -> return "REMOVE_PROVIDER"
                133 -> return "DISPATCH_PACKAGE_BROADCAST"
                134 -> return "SCHEDULE_CRASH"
                135 -> return "DUMP_HEAP"
                136 -> return "DUMP_ACTIVITY"
                138 -> return "SET_CORE_SETTINGS"
                139 -> return "UPDATE_PACKAGE_COMPATIBILITY_INFO"
                141 -> return "DUMP_PROVIDER"
                142 -> return "UNSTABLE_PROVIDER_DIED"
                143 -> return "REQUEST_ASSIST_CONTEXT_EXTRAS"
                144 -> return "TRANSLUCENT_CONVERSION_COMPLETE"
                145 -> return "INSTALL_PROVIDER"
                146 -> return "ON_NEW_ACTIVITY_OPTIONS"
                149 -> return "ENTER_ANIMATION_COMPLETE"
                154 -> return "LOCAL_VOICE_INTERACTION_STARTED"
                155 -> return "ATTACH_AGENT"
                156 -> return "APPLICATION_INFO_CHANGED"
                158 -> return "RUN_ISOLATED_ENTRY_POINT"
                159 -> return "EXECUTE_TRANSACTION"
                160 -> return "RELAUNCH_ACTIVITY"
                161 -> return "PURGE_RESOURCES"
                162 -> return "ATTACH_STARTUP_AGENTS"
                163 -> return "UPDATE_UI_TRANSLATION_STATE"
                164 -> return "SET_CONTENT_CAPTURE_OPTIONS_CALLBACK"
                165 -> return "DUMP GFXINFO"
                170 -> return "INSTRUMENT_WITHOUT_RESTART"
                171 -> return "FINISH_INSTRUMENTATION_WITHOUT_RESTART"
            }
            return code.toString()
        }
    }
}