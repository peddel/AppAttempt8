package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var socket: Socket? = null
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private var serverIp = "" // Change to your server's IP
    private val serverPort = 8080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.connectButton.setOnClickListener {
            if (!isConnected) {
                // Get IP from input field when connecting
                serverIp = binding.ipAddressInput.text.toString().trim()

                if (serverIp.isEmpty()) {
                    updateStatus("Please enter an IP address")
                    return@setOnClickListener
                }

                if (!isValidIpAddress(serverIp)) {
                    updateStatus("Invalid IP format")
                    return@setOnClickListener
                }

                connectToServer()
            } else {
                disconnectFromServer()
            }
        }
    }

    // Validate IP address format
    private fun isValidIpAddress(ip: String): Boolean {
        val pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        return ip.matches(pattern.toRegex())
    }
    private fun connectToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateStatus("Connecting...")
                socket = Socket(serverIp, serverPort)
                isConnected = true
                updateStatus("Connected to $serverIp")
                updateButtonText("Disconnect")

                val input = socket?.getInputStream()
                val reader = BufferedReader(InputStreamReader(input))

                while (isConnected) {
                    val data = reader.readLine()
                    if (data != null) {
                        printReceivedData(data)
                    } else {
                        break
                    }
                }

            } catch (e: Exception) {
                if (isConnected) {
                    updateStatus("Error: ${e.message}")
                    disconnectFromServer()
                }
            }
        }
    }

    private fun printReceivedData(jsonString: String) {
        handler.post {
            try {
                // Parse JSON
                val json = JSONObject(jsonString)
                val stringBuilder = StringBuilder()

                // Extract and format data
                json.keys().forEach { messageKey ->
                    val message = json.getJSONObject(messageKey)
                    stringBuilder.append("$messageKey:\n")

                    message.keys().forEach { signalKey ->
                        val value = message.get(signalKey)
                        stringBuilder.append("  $signalKey: $value\n")
                    }
                }

                binding.dataTextView.text = stringBuilder.toString()
                binding.dataTextView.append("\n\nRaw JSON:\n$jsonString")

            } catch (e: Exception) {
                binding.dataTextView.text = "Received: $jsonString\n\n(Error parsing JSON: ${e.message})"
            }
        }
    }

    private fun disconnectFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isConnected = false
                updateStatus("Disconnected")
                updateButtonText("Connect to Server")
            }
        }
    }

    private fun updateStatus(message: String) {
        handler.post {
            binding.statusTextView.text = "Status: $message"
        }
    }

    private fun updateButtonText(text: String) {
        handler.post {
            binding.connectButton.text = text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}


