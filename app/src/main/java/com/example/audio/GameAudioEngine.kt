package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.sin

class GameAudioEngine {
    private val sampleRate = 22050
    private val bufferSize = 1024

    var soundVolume: Float = 0.8f
    var musicVolume: Float = 0.6f
    var isMuted: Boolean = false

    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isRunning = false
    private var audioTrack: AudioTrack? = null

    private var alarmLevel: Int = 0 // 0 to 3
    private var bgmTimerSamples = 0
    private var bgmNoteIndex = 0
    private val bassNotes = floatArrayOf(55f, 55f, 65.41f, 73.42f, 55f, 55f, 82.41f, 73.42f)

    private val activeVoices = ConcurrentLinkedQueue<Voice>()

    private class Voice(
        val isNoise: Boolean,
        val frequency: Float,
        val isSawtooth: Boolean,
        val highPass: Boolean,
        val totalSamples: Int,
        var currentSample: Int = 0,
        val volume: Float
    ) {
        var lastNoiseVal: Float = 0f
    }

    init {
        startStreamingAudio()
    }

    private fun startStreamingAudio() {
        if (isRunning) return
        isRunning = true

        audioScope.launch {
            try {
                val minBufSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(bufferSize * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val pcmBuffer = ShortArray(bufferSize)
                val rng = java.util.Random()

                while (isActive && isRunning) {
                    // 1. Check Music Step Sequencer
                    if (isBgmActive && musicVolume > 0.01f && !isMuted) {
                        val noteIntervalSamples = if (alarmLevel > 0) (sampleRate * 0.22f).toInt() else (sampleRate * 0.40f).toInt()
                        bgmTimerSamples += bufferSize
                        if (bgmTimerSamples >= noteIntervalSamples) {
                            bgmTimerSamples = 0
                            val freq = bassNotes[bgmNoteIndex % bassNotes.size]
                            bgmNoteIndex++
                            val durationSamples = if (alarmLevel > 0) (sampleRate * 0.18f).toInt() else (sampleRate * 0.32f).toInt()
                            addVoice(
                                Voice(
                                    isNoise = false,
                                    frequency = freq,
                                    isSawtooth = true,
                                    highPass = false,
                                    totalSamples = durationSamples,
                                    volume = musicVolume * 0.35f
                                )
                            )
                        }
                    }

                    // 2. Synthesize & Mix Voices into PCM Buffer
                    if (activeVoices.isEmpty() || isMuted) {
                        pcmBuffer.fill(0)
                    } else {
                        pcmBuffer.fill(0)
                        val iterator = activeVoices.iterator()
                        while (iterator.hasNext()) {
                            val voice = iterator.next()
                            val voiceSamplesRemaining = voice.totalSamples - voice.currentSample
                            val samplesToProcess = minOf(bufferSize, voiceSamplesRemaining)

                            for (i in 0 until samplesToProcess) {
                                val curS = voice.currentSample + i
                                val envelope = (1.0f - (curS.toFloat() / voice.totalSamples)).coerceIn(0f, 1f)

                                val sampleValue: Float = if (voice.isNoise) {
                                    val raw = (rng.nextFloat() * 2f - 1f) * voice.volume
                                    if (voice.highPass) {
                                        val hp = raw - voice.lastNoiseVal
                                        voice.lastNoiseVal = raw
                                        hp * envelope
                                    } else {
                                        raw * envelope
                                    }
                                } else {
                                    val t = curS.toDouble() / sampleRate
                                    if (voice.isSawtooth) {
                                        val period = 1.0 / voice.frequency
                                        val phase = (t % period) / period
                                        ((2.0 * phase - 1.0) * voice.volume * envelope).toFloat()
                                    } else {
                                        (sin(2.0 * PI * voice.frequency * t) * voice.volume * envelope).toFloat()
                                    }
                                }

                                val mixed = (pcmBuffer[i] + (sampleValue * 32767).toInt()).coerceIn(-32768, 32767)
                                pcmBuffer[i] = mixed.toShort()
                            }

                            voice.currentSample += samplesToProcess
                            if (voice.currentSample >= voice.totalSamples) {
                                iterator.remove()
                            }
                        }
                    }

                    track.write(pcmBuffer, 0, bufferSize)
                }
            } catch (_: Exception) {
                // Audio track streaming graceful teardown
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
                audioTrack = null
            }
        }
    }

    private var isBgmActive = false

    fun setAlarmLevel(level: Int) {
        alarmLevel = level
    }

    fun startBgm() {
        isBgmActive = true
    }

    fun stopBgm() {
        isBgmActive = false
        bgmTimerSamples = 0
    }

    private fun addVoice(voice: Voice) {
        if (activeVoices.size < 16) {
            activeVoices.add(voice)
        }
    }

    fun playShootPistol() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(
            Voice(
                isNoise = true,
                frequency = 0f,
                isSawtooth = false,
                highPass = true,
                totalSamples = (sampleRate * 0.04f).toInt(),
                volume = soundVolume * 0.5f
            )
        )
    }

    fun playShootSmg() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(
            Voice(
                isNoise = true,
                frequency = 0f,
                isSawtooth = false,
                highPass = false,
                totalSamples = (sampleRate * 0.06f).toInt(),
                volume = soundVolume * 0.8f
            )
        )
    }

    fun playStunnerZap() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(
            Voice(
                isNoise = false,
                frequency = 880f,
                isSawtooth = true,
                highPass = false,
                totalSamples = (sampleRate * 0.09f).toInt(),
                volume = soundVolume * 0.7f
            )
        )
        addVoice(
            Voice(
                isNoise = false,
                frequency = 440f,
                isSawtooth = false,
                highPass = false,
                totalSamples = (sampleRate * 0.08f).toInt(),
                volume = soundVolume * 0.5f
            )
        )
    }

    fun playEmpBlast() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(
            Voice(
                isNoise = true,
                frequency = 0f,
                isSawtooth = false,
                highPass = false,
                totalSamples = (sampleRate * 0.18f).toInt(),
                volume = soundVolume * 1.0f
            )
        )
        addVoice(
            Voice(
                isNoise = false,
                frequency = 120f,
                isSawtooth = true,
                highPass = false,
                totalSamples = (sampleRate * 0.25f).toInt(),
                volume = soundVolume * 0.8f
            )
        )
    }

    fun playFootstep() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(
            Voice(
                isNoise = true,
                frequency = 0f,
                isSawtooth = false,
                highPass = true,
                totalSamples = (sampleRate * 0.02f).toInt(),
                volume = soundVolume * 0.15f
            )
        )
    }

    fun playHackBeep(isSuccess: Boolean) {
        if (soundVolume <= 0.01f || isMuted) return
        if (isSuccess) {
            addVoice(Voice(false, 523.25f, false, false, (sampleRate * 0.06f).toInt(), 0, soundVolume * 0.6f))
            addVoice(Voice(false, 659.25f, false, false, (sampleRate * 0.08f).toInt(), 0, soundVolume * 0.6f))
            addVoice(Voice(false, 783.99f, false, false, (sampleRate * 0.10f).toInt(), 0, soundVolume * 0.7f))
        } else {
            addVoice(Voice(false, 220f, true, false, (sampleRate * 0.12f).toInt(), 0, soundVolume * 0.6f))
            addVoice(Voice(false, 180f, true, false, (sampleRate * 0.16f).toInt(), 0, soundVolume * 0.7f))
        }
    }

    fun playUiClick() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(Voice(false, 987.77f, false, false, (sampleRate * 0.025f).toInt(), 0, soundVolume * 0.4f))
    }

    fun playAlarm() {
        if (soundVolume <= 0.01f || isMuted) return
        addVoice(Voice(false, 600f, true, false, (sampleRate * 0.08f).toInt(), 0, soundVolume * 0.8f))
        addVoice(Voice(false, 750f, true, false, (sampleRate * 0.08f).toInt(), 0, soundVolume * 0.8f))
    }

    fun release() {
        isRunning = false
        audioScope.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
