package com.example.chatdz.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    var currentlyPlayingUri: String? = null
        private set

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "audio_messages").apply { mkdirs() }
            val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
            false
        }
    }

    fun stopRecording(): Pair<String, Int>? {
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Error stopping recorder", e)
                }
                release()
            }
            mediaRecorder = null

            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                Pair(file.absolutePath, durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error finalizing recording", e)
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
            mediaRecorder = null
            currentRecordingFile?.delete()
            currentRecordingFile = null
        } catch (_: Exception) {}
    }

    var currentSpeed: Float = 1.0f
        private set

    fun cycleSpeed(): Float {
        currentSpeed = when (currentSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        setPlaybackSpeed(currentSpeed)
        return currentSpeed
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error updating speed", e)
            }
        }
    }

    fun playAudio(uri: String, speed: Float = currentSpeed, onCompletion: () -> Unit) {
        stopPlayback()
        currentSpeed = speed
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(uri)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && speed != 1.0f) {
                    try {
                        playbackParams = playbackParams.setSpeed(speed)
                    } catch (_: Exception) {}
                }
                setOnCompletionListener {
                    currentlyPlayingUri = null
                    onCompletion()
                }
                start()
            }
            currentlyPlayingUri = uri
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio", e)
            currentlyPlayingUri = null
            onCompletion()
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentlyPlayingUri = null
    }

    fun seekToPercent(percent: Float) {
        try {
            mediaPlayer?.let { player ->
                val duration = player.duration
                if (duration > 0) {
                    val targetMs = (duration * percent.coerceIn(0f, 1f)).toInt()
                    player.seekTo(targetMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun getCurrentProgress(): Float {
        return try {
            val player = mediaPlayer ?: return 0f
            val dur = player.duration
            if (dur > 0) player.currentPosition.toFloat() / dur else 0f
        } catch (_: Exception) {
            0f
        }
    }

    fun isPlaying(uri: String): Boolean {
        return currentlyPlayingUri == uri && (mediaPlayer?.isPlaying == true)
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
