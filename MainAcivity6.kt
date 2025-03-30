package com.example.jarvisai

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class JarvisService : Service(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private val client = OkHttpClient()
    private val openAiApiKey = "YOUR_OPENAI_API_KEY" // Replace with your API key
    private val openAiUrl = "https://api.openai.com/v1/chat/completions"

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        startListening()
    }

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.let {
                    for (result in it) {
                        if (result.lowercase(Locale.ROOT).contains("jarvis")) {
                            speak("Yes, how can I assist you?")
                            getChatGPTResponse(result.replace("jarvis", ""))
                        }
                    }
                }
                startListening()
            }
            override fun onError(error: Int) {
                startListening()
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    private fun getChatGPTResponse(userMessage: String) {
        val jsonBody = JSONObject()
        jsonBody.put("model", "gpt-4")
        jsonBody.put("messages", listOf(JSONObject().put("role", "user").put("content", userMessage)))
        val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString())

        val request = Request.Builder()
            .url(openAiUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $openAiApiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                speak("I couldn't connect to the AI. Please try again later.")
            }
            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let {
                    val jsonResponse = JSONObject(it)
                    val chatResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    speak(chatResponse)
                }
            }
        })
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.ENGLISH
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        speechRecognizer.destroy()
        textToSpeech.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
