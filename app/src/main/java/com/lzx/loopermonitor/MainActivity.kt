package com.lzx.loopermonitor

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.lze.loopermonitor.R
import com.lzx.library.LooperMonitor

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn).setOnClickListener {
            findViewById<Button>(R.id.btn).text = "dump(" + LooperMonitor.getEntries().size + ")"
            LooperMonitor.getEntries().forEach {
                Log.i("MainActivity", it.toString())
            }
        }
    }

}