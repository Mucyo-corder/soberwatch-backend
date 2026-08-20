package com.example.services

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class VoiceService(private val context: Context) : RecognitionListener {

  private var speechRecognizer: SpeechRecognizer? = null
  private var tts: TextToSpeech? = null
  private var mediaRecorder: MediaRecorder? = null
  private var mediaPlayer: MediaPlayer? = null

  private val _isListening = MutableStateFlow(false)
  val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  private val _isRecordingNote = MutableStateFlow(false)
  val isRecordingNote: StateFlow<Boolean> = _isRecordingNote.asStateFlow()

  private val _spokenText = MutableStateFlow("")
  val spokenText: StateFlow<String> = _spokenText.asStateFlow()

  private var isTtsInitialized = false
  private var currentAudioPath: String? = null

  init {
    initializeTts()
  }

  private fun initializeTts() {
    tts = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts?.language = Locale.US
        isTtsInitialized = true
      }
    }
  }

  // --- Speech Recognition ---

  fun startListening() {
    if (speechRecognizer == null) {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
      speechRecognizer?.setRecognitionListener(this)
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    speechRecognizer?.startListening(intent)
    _isListening.value = true
  }

  fun stopListening() {
    speechRecognizer?.stopListening()
    _isListening.value = false
  }

  // --- Voice Note Recording ---

  fun startRecordingNote(): String {
    val audioFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
    currentAudioPath = audioFile.absolutePath

    mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      MediaRecorder(context)
    } else {
      MediaRecorder()
    }.apply {
      setAudioSource(MediaRecorder.AudioSource.MIC)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      setOutputFile(currentAudioPath)
      prepare()
      start()
    }
    
    _isRecordingNote.value = true
    return currentAudioPath!!
  }

  fun stopRecordingNote(): String? {
    try {
      mediaRecorder?.stop()
      mediaRecorder?.release()
    } catch (e: Exception) {
      // Handle cases where recording was too short
    }
    mediaRecorder = null
    _isRecordingNote.value = false
    return currentAudioPath
  }

  fun playAudio(path: String) {
    mediaPlayer?.stop()
    mediaPlayer?.release()
    mediaPlayer = MediaPlayer().apply {
      setDataSource(path)
      prepare()
      start()
    }
  }

  // --- Text to Speech ---

  fun speak(text: String) {
    if (isTtsInitialized) {
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
  }

  fun shutdown() {
    speechRecognizer?.destroy()
    tts?.stop()
    tts?.shutdown()
    mediaRecorder?.release()
    mediaPlayer?.release()
  }

  // RecognitionListener overrides
  override fun onReadyForSpeech(params: Bundle?) {}
  override fun onBeginningOfSpeech() {}
  override fun onRmsChanged(rmsdB: Float) {}
  override fun onBufferReceived(buffer: ByteArray?) {}
  override fun onEndOfSpeech() {
    _isListening.value = false
  }

  override fun onError(error: Int) {
    _isListening.value = false
  }

  override fun onResults(results: Bundle?) {
    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    if (!matches.isNullOrEmpty()) {
      _spokenText.value = matches[0]
    }
    _isListening.value = false
  }

  override fun onPartialResults(partialResults: Bundle?) {}
  override fun onEvent(eventType: Int, params: Bundle?) {}
}
