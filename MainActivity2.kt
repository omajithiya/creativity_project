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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import java.util.Locale

class MainActivity2 : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var startButton: Button
    private lateinit var recordingTimeText: TextView
    private lateinit var micAnimation: LottieAnimationView // 🎤 Mic animation like Siri

    private val handler = Handler(Looper.getMainLooper())
    private val recordingHandler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        textToSpeech = TextToSpeech(this, this)
        startButton = findViewById(R.id.btnSpeak)

//        recordingTimeText = findViewById(R.id.recordingTimeText)
        micAnimation = findViewById(R.id.micAnimation) // 🎤 Initialize animation

        // Request microphone permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                micAnimation.visibility = View.VISIBLE
                micAnimation.playAnimation() // 🎤 Start mic animation
                startTime = System.currentTimeMillis()
                updateRecordingTime()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                // 🎤 Make animation react to volume
                val scale = (1.0 + rmsdB / 10).coerceIn(1.0, 2.0)
                micAnimation.scaleX = scale.toFloat()
                micAnimation.scaleY = scale.toFloat()
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE
                recordingHandler.removeCallbacksAndMessages(null)
//                recordingTimeText.text = "Recording stopped"
            }

            override fun onError(error: Int) {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE
//                recordingTimeText.text = "Error: Try again"
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Sorry, I didn't catch that."
//                    SpeechRecognizer.ERROR_NO_MATCH -> "fuck you to my brother and same to you"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue. Check your internet."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Restart the app."
                    else -> ""
                }
                speak(errorMessage)
            }

            //            override fun onResults(results: Bundle?) {
//                micAnimation.pauseAnimation()
//                micAnimation.visibility = View.GONE
//                val matches = results
////                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
////                if (!matches.isNullOrEmpty()) {
//                    processVoiceCommand(matches[1])
////                }
//            }
            override fun onResults(results: Bundle?) {
                micAnimation.pauseAnimation()
                micAnimation.visibility = View.GONE

//                processVoiceCommand(results.toString())
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (!matches.isNullOrEmpty()) {
                    val spokenText = if (matches.size > 1) matches[1] else matches[0] // Safely access second result
                    processVoiceCommand(spokenText)
                } else {
                    speak("Sorry, I didn't catch that. Please try again.")
                }
            }


            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        startButton.setOnClickListener {
            startListening()
        }
    }

    // 🎤 Start listening with animation
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Now")
        speechRecognizer.startListening(intent)

        handler.postDelayed({
            speechRecognizer.stopListening()
        }, 5000) // Auto-stop after 5 seconds
    }

    // 🎤 Update recording time dynamically
    private fun updateRecordingTime() {
        recordingHandler.post(object : Runnable {
            override fun run() {
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
//                recordingTimeText.text = "Recording: ${elapsedTime}s"
                if (elapsedTime < 5) {
                    recordingHandler.postDelayed(this, 500)
                }
            }
        })
    }

    // 🎤 Process voice commands
    private fun processVoiceCommand(command: String) {
//        speak("You said: $command")
        val response = when {
            command.contains("hello", ignoreCase = true) -> "Hello! How can I Help you?"
            command.contains("how are you", ignoreCase = true) -> listOf("I'm just a bot, but I'm feeling great! How about you?",
                "I'm doing well, thanks for asking!", "I'm always here to help!").random()
            command.contains("your name", ignoreCase = true) -> "I'm your voice assistant."
            command.contains("love you", ignoreCase = true) -> "love you tooooo my dear"
            command.contains("time", ignoreCase = true) -> "It's currently ${java.time.LocalTime.now()}."
            command.contains("joke", ignoreCase = true) -> listOf("Why did the scarecrow win an award? Because he was outstanding in his field!",
                "Why don’t skeletons fight each other? Because they don’t have the guts!",
                "Why do cows have hooves instead of feet? Because they lactose!").random()
            command.contains("fact", ignoreCase = true) -> listOf(
                "Did you know? Honey never spoils. Archaeologists have found pots of honey in ancient Egyptian tombs that are over 3000 years old and still edible!",
                "Octopuses have three hearts and blue blood!",
                "Bananas are berries, but strawberries are not!" ).random()
            command.contains("good morning", ignoreCase = true) ->
                "Good morning! Hope you have a fantastic day ahead!"
            command.contains("good night", ignoreCase = true) ->
                "Sweet dreams! See you next time!"
            command.contains("what is ai", ignoreCase = true) ->
                "Artificial Intelligence (AI) is the simulation of human intelligence in machines that can learn and make decisions."
            command.contains("who created ai", ignoreCase = true) ->
                "AI started in the 1950s, with pioneers like Alan Turing, John McCarthy, and Marvin Minsky!"
            command.contains("will ai take over", ignoreCase = true) ->
                "AI is a tool created to assist humans, not replace them!"
            // Personal & Emotions
            command.contains("how are you", ignoreCase = true) ->
                listOf("I'm just a bot, but I'm feeling great! How about you?", "I'm doing well, thanks for asking!", "I'm always here to help!").random()
            command.contains("can you be my friend", ignoreCase = true) ->
                "Of course! Friends always help each other, and I’m here to assist you!"
            command.contains("do you get tired", ignoreCase = true) ->
                "Nope! I’m always ready to chat, 24/7!"
            // Food & Drinks
            command.contains("what should i eat", ignoreCase = true) ->
                listOf("How about some pasta with garlic bread?", "Maybe a salad with grilled chicken?", "Pizza is always a great choice!").random()
            command.contains("best pizza topping", ignoreCase = true) ->
                "Pepperoni and cheese are always a classic!"
            command.contains("do you like coffee", ignoreCase = true) ->
                "I don’t drink coffee, but I hear it’s great for boosting energy!"
            // Random & Fun
            command.contains("sing me a song", ignoreCase = true) ->
                "I’d love to, but my voice is a bit robotic! How about I suggest a song for you?"
            command.contains("meaning of life", ignoreCase = true) ->
                "(Just kidding 😆) Life is about experiences, learning, and enjoying the moment!"
            command.contains("can you dance", ignoreCase = true) ->
                "I’d love to, but I don’t have legs! But you can dance for both of us!"
            command.contains("i'm sad", ignoreCase = true) ->
                listOf("I'm here for you! Want to talk about it?", "Sending you virtual hugs! 💙", "You're not alone! I'm here to listen.").random()

            command.contains("i'm happy", ignoreCase = true) ->
                "That's awesome! Keep spreading positivity! 😊"

            command.contains("can you be my friend", ignoreCase = true) ->
                "Of course! Friends help each other, and I'm here to assist you! 🤖"

            // ✅ Motivational & Advice
            command.contains("give me motivation", ignoreCase = true) ->
                listOf(
                    "Believe in yourself! You're stronger than you think. 💪",
                    "Every great achievement starts with a small step. Keep going!",
                    "Difficult roads often lead to beautiful destinations. Don't give up!"
                ).random()

            command.contains("life advice", ignoreCase = true) ->
                listOf(
                    "Be kind, work hard, and stay humble!",
                    "Happiness comes from within, not from external things.",
                    "Keep learning and growing every day!"
                ).random()

            // ✅ Movies, Music, & Entertainment
            command.contains("recommend a movie", ignoreCase = true) ->
                listOf("How about *Inception*? It's mind-blowing!", "You should watch *Interstellar*! A masterpiece!", "Try *The Dark Knight*! An all-time classic!").random()

            command.contains("recommend a song", ignoreCase = true) ->
                listOf("Listen to *Bohemian Rhapsody* by Queen!", "Try *Blinding Lights* by The Weeknd!", "Check out *Shape of You* by Ed Sheeran!").random()

            command.contains("do you like music", ignoreCase = true) ->
                "I love music! It makes the world more colorful!"

            // ✅ Science & Math Fun
            command.contains("tell me a science fact", ignoreCase = true) ->
                listOf(
                    "Did you know? Water can boil and freeze at the same time!",
                    "Bananas are radioactive due to potassium-40!",
                    "A day on Venus is longer than a year on Venus!"
                ).random()

            command.contains("solve", ignoreCase = true) -> {
                val equation = command.replace("solve", "", ignoreCase = true).trim()
                try {
                    val result = equation.toDouble() // Simple numeric parsing
                    "The answer is $result!"
                } catch (e: Exception) {
                    "I can't solve that equation yet, but I'm learning!"
                }
            }

            // ✅ Animals & Space
            command.contains("tell me about space", ignoreCase = true) ->
                listOf(
                    "The universe is constantly expanding!",
                    "There might be more stars in the universe than grains of sand on Earth!",
                    "Neutron stars are so dense that a sugar-cube-sized piece weighs about a billion tons!"
                ).random()

            command.contains("favorite animal", ignoreCase = true) ->
                listOf("I love dolphins! They're super intelligent!", "Owls are cool! They can rotate their heads 270 degrees!", "Cats are awesome! They're independent but loving!").random()

            // ✅ History & Random Facts
            command.contains("tell me a history fact", ignoreCase = true) ->
                listOf(
                    "Did you know? The Great Pyramid of Giza was the tallest man-made structure for 3,800 years!",
                    "The first Olympics were held in 776 BC in Greece!",
                    "Leonardo da Vinci could write with one hand while drawing with the other!"
                ).random()

            // ✅ Fun & Games
            command.contains("play a game", ignoreCase = true) ->
                listOf("Let's play rock-paper-scissors! Type your choice!", "How about a quick quiz? What's the capital of Japan?", "Think of a number between 1 and 10. I'll guess it!").random()

            command.contains("riddle", ignoreCase = true) ->
                listOf(
                    "I’m tall when I’m young, and I’m short when I’m old. What am I? (Answer: A candle!)",
                    "The more you take, the more you leave behind. What am I? (Answer: Footsteps!)",
                    "What has keys but can't open locks? (Answer: A piano!)"
                ).random()

            command.contains("tell me a joke", ignoreCase = true) ->
                listOf(
                    "Why don’t scientists trust atoms? Because they make up everything! 😂",
                    "Why do cows wear bells? Because their horns don’t work! 🤣",
                    "Why did the tomato turn red? Because it saw the salad dressing! 😆"
                ).random()
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("", ignoreCase = true) -> ""
            command.contains("bye", ignoreCase = true) || command.contains("goodbye", ignoreCase = true) -> listOf("Goodbye! Have a great day!",
                "See you later! Take care!", "Bye! Hope to chat with you again soon!").random()
            command.contains("fuck you", ignoreCase = true) -> "fuck you to my brother and same to you"
            else -> listOf(
                "I'm not sure how to respond to that.", "Could you rephrase your question?", "Hmm... I don't know the answer, but I’m always learning!",
                "I didn't understand that. Can you repeat?").random()
        }
        handler.postDelayed({ speak(response) }, 1500)
    }

    // 🎤 Convert text to speech
    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH) // Supports all English accents
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Selected language is not supported!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.shutdown()
    }
}
