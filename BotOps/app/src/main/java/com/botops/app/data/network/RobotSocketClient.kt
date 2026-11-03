package com.botops.app.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket

class RobotSocketClient(private val context: Context? = null) {
    companion object {
        private const val TAG = "RobotSocketClient"
        private const val DEFAULT_HOST = "192.168.1.5"
        private const val DEFAULT_PORT = 8080
    }

    private fun getHost(): String {
        return context?.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            ?.getString("simulator_host", DEFAULT_HOST) ?: DEFAULT_HOST
    }

    private fun getPort(): Int {
        return context?.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            ?.getInt("simulator_port", DEFAULT_PORT) ?: DEFAULT_PORT
    }

    /**
     * Sends a string command to the desktop robot simulator over a TCP socket.
     * Operates safely on Dispatchers.IO with connection timeout and try-catch safety.
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val host = getHost()
        val port = getPort()
        var socket: Socket? = null
        var writer: PrintWriter? = null
        try {
            Log.d(TAG, "Attempting connection to $host:$port...")
            socket = Socket(host, port).apply {
                soTimeout = 5000 // 5-second timeout
            }
            writer = PrintWriter(socket.getOutputStream(), true)
            writer.println(command)
            writer.flush()
            Log.d(TAG, "Successfully sent command to $host:$port -> $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "SOCKET ERROR connecting to $host:$port -> ${e.javaClass.simpleName}: ${e.message}", e)
            false
        } finally {
            try {
                writer?.close()
                socket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket resources", e)
            }
        }
    }
}
