package com.example.voicerecipeassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity2 : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var startButton: Button
    private lateinit var micAnimation: LottieAnimationView
    private val handler = Handler(Looper.getMainLooper())
    private val responseCache = mutableMapOf<String, String>()

    private val openAiApiKey = "your_openai_api_key" // REPLACE with your OpenAI API Key
    private val openAiUrl = "https://api.openai.com/v1/chat/completions"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        textToSpeech = TextToSpeech(this, this)
        startButton = findViewById(R.id.btnSpeak)
        micAnimation = findViewById(R.id.micAnimation)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                micAnimation.visibility = View.VISIBLE
                micAnimation.playAnimation()
            }

            override fun onEndOfSpeech() {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE
            }

            override fun onError(error: Int) {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE
                speak("Sorry, I didn't understand that. Please try again.")
            }

            override fun onResults(results: Bundle?) {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val userMessage = matches[0].lowercase(Locale.ROOT)
                    if (userMessage.contains("jarvis")) {
                        val cleanedMessage = userMessage.replace("jarvis", "").trim()
                        getAIResponse(cleanedMessage)
                    } else {
                        speak("Say 'Jarvis' before your command.")
                    }
                } else {
                    speak("Sorry, I didn't catch that. Please try again.")
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startButton.setOnClickListener { startListening() }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Jarvis' followed by your command.")
        speechRecognizer.startListening(intent)

        handler.postDelayed({ speechRecognizer.stopListening() }, 5000)
    }

    private fun getAIResponse(userMessage: String) {
        val cachedResponse = responseCache[userMessage]
        if (cachedResponse != null) {
            speak(cachedResponse)
            return
        }

        val client = OkHttpClient()
        val jsonBody = JSONObject()
        jsonBody.put("model", "gpt-4")
        jsonBody.put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        })
        jsonBody.put("temperature", 0.7)

        val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString())

        val request = Request.Builder()
            .url(openAiUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $openAiApiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { speak("I couldn't connect to the AI. Please try again later.") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let {
                    val jsonResponse = JSONObject(it)
                    val chatResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    responseCache[userMessage] = chatResponse
                    runOnUiThread {
                        speak(chatResponse)
                        handler.postDelayed({ startListening() }, 2000) // Auto-restart listening
                    }
                }
            }
        })
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Selected language is not supported!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}
