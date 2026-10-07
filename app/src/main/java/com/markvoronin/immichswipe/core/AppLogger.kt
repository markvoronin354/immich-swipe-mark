package com.markvoronin.immichswipe.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object AppLogger {
    private const val TAG = "AppLogger"
    private const val CURRENT_LOG_FILE = "current_logs.txt"
    private const val PREVIOUS_LOG_FILE = "previous_logs.txt"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1 MB

    private var logsDir: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logChannel = Channel<String>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (line in logChannel) {
                writeToFile(line)
            }
        }
    }


    fun init(context: Context) {
        this.logsDir = context.applicationContext.filesDir
        enqueueRaw("\n\n" + "=".repeat(50) + "\n" + "   NEW SESSION START   \n" + "=".repeat(50) + "\n\n")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        enqueue("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        enqueue("I", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        enqueue("W", tag, "$message ${throwable?.stackTraceToString() ?: ""}")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        enqueue("E", tag, "$message ${throwable?.stackTraceToString() ?: ""}")
    }

    private fun enqueueRaw(text: String) {
        logChannel.trySend(text)
    }

    private fun enqueue(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "$timestamp $level/$tag: $message\n"
        logChannel.trySend(logLine)
    }

    private fun writeToFile(text: String) {
        val dir = logsDir ?: return
        try {
            val currentFile = File(dir, CURRENT_LOG_FILE)


            if (currentFile.exists() && currentFile.length() > MAX_FILE_SIZE) {
                val previousFile = File(dir, PREVIOUS_LOG_FILE)
                if (previousFile.exists()) previousFile.delete()
                currentFile.renameTo(previousFile)
            }

            FileOutputStream(currentFile, true).use {
                it.write(text.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file", e)
        }
    }


    private fun flushPendingLogs() {
        while (true) {
            val line = logChannel.tryReceive().getOrNull() ?: break
            writeToFile(line)
        }
    }


    @Synchronized
    fun getLogs(): String {
        flushPendingLogs()
        val dir = logsDir ?: return "Logger not initialized"
        val currentFile = File(dir, CURRENT_LOG_FILE)
        val previousFile = File(dir, PREVIOUS_LOG_FILE)

        val logs = StringBuilder()
        if (previousFile.exists()) {
            logs.append("--- PREVIOUS LOGS ---\n")
            try {
                logs.append(previousFile.readText())
            } catch (e: Exception) {
                logs.append("Error reading previous logs: ${e.message}\n")
            }
            logs.append("\n\n")
        }

        if (currentFile.exists()) {
            logs.append("--- CURRENT LOGS ---\n")
            try {
                logs.append(currentFile.readText())
            } catch (e: Exception) {
                logs.append("Error reading current logs: ${e.message}\n")
            }
        }

        return if (logs.isEmpty()) "No logs available" else logs.toString()
    }


    @Synchronized
    fun clearLogs() {
        flushPendingLogs()
        val dir = logsDir ?: return
        try {
            File(dir, CURRENT_LOG_FILE).delete()
            File(dir, PREVIOUS_LOG_FILE).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}
