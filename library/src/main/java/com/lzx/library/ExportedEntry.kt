package com.lzx.library

/**
 * 对外的Entry
 */
class ExportedEntry(entry: Entry) {
    var handlerClassName: String? = null
    var threadName: String? = null
    var messageName: String? = null
    var messageCount: Long = 0
    var recordedMessageCount: Long = 0
    var exceptionCount: Long = 0
    var totalLatencyMicros: Long = 0
    var maxLatencyMicros: Long = 0
    var latencyMicro: Long = 0
    var maxDelayMillis: Long = 0
    var delayMillis: Long = 0
    var recordedDelayMessageCount: Long = 0
    var target: String? = null
    var what: String? = null
    var mHMsgWhat: String? = null
    var msgType: String? = null //

    init {
        if (entry.handler != null) {
            handlerClassName = entry.handler?.javaClass?.name
            threadName = entry.handler?.looper?.thread?.name
        } else {
            handlerClassName = entry.messageName
        }
        messageName = entry.messageName
        messageCount = entry.messageCount
        recordedMessageCount = entry.recordedMessageCount
        exceptionCount = entry.exceptionCount
        latencyMicro = entry.latencyMicro
        totalLatencyMicros = entry.totalLatencyMicro
        maxLatencyMicros = entry.maxLatencyMicro
        delayMillis = entry.delayMillis
        maxDelayMillis = entry.maxDelayMillis
        recordedDelayMessageCount = entry.recordedDelayMessageCount
        target = entry.msgTarget
        what = entry.msgWhat
        mHMsgWhat = entry.mHMsgWhat
    }

    override fun toString(): String {
        return "{\n" +
            "    \"handlerClassName\":" + handlerClassName + ",\n" +
            "    \"threadName\":" + threadName + ",\n" +
            "    \"messageName\":" + messageName + ",\n" +
            "    \"messageCount\":" + messageCount + ",\n" +
            "    \"recordedMessageCount\":" + recordedMessageCount + ",\n" +
            "    \"exceptionCount\":" + exceptionCount + ",\n" +
            "    \"latencyMicro\":" + latencyMicro + ",\n" +
            "    \"totalLatencyMicros\":" + totalLatencyMicros + ",\n" +
            "    \"maxLatencyMicros\":" + maxLatencyMicros + ",\n" +
            "    \"maxDelayMillis\":" + maxDelayMillis + ",\n" +
            "    \"delayMillis\":" + delayMillis + ",\n" +
            "    \"recordedDelayMessageCount\":" + recordedDelayMessageCount + ",\n" +
            "    \"target\":" + target + ",\n" +
            "    \"what\":" + what + ",\n" +
            "    \"mHMsgWhat\":" + mHMsgWhat + "\n" +
            "}\n" +
            "    ----------------------------------------------------"
    }
}