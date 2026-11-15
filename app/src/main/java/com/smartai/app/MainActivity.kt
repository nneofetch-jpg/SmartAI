package com.smartai.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.text.InputType
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection

class MainActivity : Activity() {

    private lateinit var apiKey: EditText
    private lateinit var question: EditText
    private lateinit var answer: TextView
    private lateinit var send: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 30, 24, 20)
        layout.setBackgroundColor(Color.rgb(15, 20, 35))

        val title = TextView(this)
        title.text = "🤖 SMART AI"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "Your Personal AI Assistant"
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER
        layout.addView(subtitle)

        apiKey = EditText(this)
        apiKey.hint = "Enter your OpenAI API Key"
        apiKey.setSingleLine(true)
        apiKey.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(apiKey)

        val scroll = ScrollView(this)
        answer = TextView(this)
        answer.text = "Welcome to Smart AI! ❤️"
        answer.textSize = 17f
        answer.setTextColor(Color.WHITE)
        answer.setPadding(8, 20, 8, 20)
        scroll.addView(answer)

        layout.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        question = EditText(this)
        question.hint = "Ask Smart AI anything..."
        question.setTextColor(Color.WHITE)
        question.setHintTextColor(Color.LTGRAY)
        layout.addView(question)

        send = Button(this)
        send.text = "SEND 🚀"
        layout.addView(send)

        send.setOnClickListener {
            askAI()
        }

        setContentView(layout)
    }

    private fun askAI() {
        val key = apiKey.text.toString().trim()
        val text = question.text.toString().trim()

        if (key.isEmpty() || text.isEmpty()) {
            Toast.makeText(
                this, "Enter API Key and your question",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        send.isEnabled = false
        answer.text = "Smart AI is thinking... 🤖"

        Thread {
            val result = try {
                callAI(key, text)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }

            runOnUiThread {
                answer.text = result
                send.isEnabled = true
            }
        }.start()
    }

    private fun callAI(key: String, text: String): String {
        val url = URL("https://api.openai.com/v1/responses")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization", "Bearer $key"
            )
            connection.setRequestProperty(
                "Content-Type", "application/json"
            )

            val body = JSONObject().apply {
                put("model", "gpt-5.5")
                put(
                    "instructions",
                    "Your name is Smart AI. Be helpful, friendly " +
                    "and answer in the user's language."
                )
                put("input", text)
            }

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val raw = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(raw)

            if (code !in 200..299) {
                return "API Error $code: " +
                    (json.optJSONObject("error")
                        ?.optString("message") ?: raw)
            }

            val output = json.optJSONArray("output")
                ?: return "No response received."

            val result = StringBuilder()

            for (i in 0 until output.length()) {
                val item = output.getJSONObject(i)

                if (item.optString("type") == "message") {
                    val content = item.optJSONArray("content") ?: continue

                    for (j in 0 until content.length()) {
                        val part = content.getJSONObject(j)
                        if (part.optString("type") == "output_text") {
                            result.append(part.optString("text"))
                        }
                    }
                }
            }

            return result.toString().ifBlank {
                "No text response received."
            }

        } finally {
            connection.disconnect()
        }
    }
}
