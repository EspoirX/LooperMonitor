package com.lzx.library

class DispatchSession {
    var startTimeMicro: Long = 0      //开机到现在包括系统深度睡眠的微秒
    var systemUptimeMillis: Long = 0  //开机到现在不包括深度睡眠的毫秒

    companion object {
        val NOT_SAMPLED = DispatchSession()
    }
}



