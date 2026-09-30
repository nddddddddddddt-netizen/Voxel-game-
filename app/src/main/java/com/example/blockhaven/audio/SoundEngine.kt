package com.example.blockhaven.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.blockhaven.world.BlockSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEngine(private val scope: CoroutineScope) {
    private val sampleRate = 22050
    private val rand = Random()

    var masterVolume = 0.8f
    var sfxVolume = 0.8f
    var musicVolume = 0.5f
    var ambientVolume = 0.4f

    private var musicJob: Job? = null
    private var ambientJob: Job? = null

    fun start() {
        startAmbientMusicLoop()
    }

    fun stop() {
        musicJob?.cancel()
        ambientJob?.cancel()
    }

    private fun playPcm(buffer: ShortArray, volumeMultiplier: Float) {
        if (masterVolume <= 0.01f || volumeMultiplier <= 0.01f) return
        val finalVol = (masterVolume * volumeMultiplier).coerceIn(0f, 1f)

        scope.launch(Dispatchers.Default) {
            try {
                val scaled = ShortArray(buffer.size)
                for (i in buffer.indices) {
                    scaled[i] = (buffer[i] * finalVol).toInt().coerceIn(-32767, 32767).toShort()
                }

                val audioTrack = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(scaled.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(scaled, 0, scaled.size)
                audioTrack.play()

                // Release track after playback completes
                delay((scaled.size.toFloat() / sampleRate * 1000f).toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio track init failures on background devices
            }
        }
    }

    fun playFootstep(soundType: BlockSoundType) {
        val durationMs = 80
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        when (soundType) {
            BlockSoundType.GRASS -> {
                // Soft muffled rustle
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = (1f - t) * exp(-t * 3f)
                    val noise = (rand.nextFloat() * 2f - 1f) * 0.4f
                    val tone = sin(2f * PI.toFloat() * 120f * (i.toFloat() / sampleRate)) * 0.3f
                    buffer[i] = ((noise + tone) * env * 16000f).toInt().toShort()
                }
            }
            BlockSoundType.STONE -> {
                // Sharp click with fast decay
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = exp(-t * 12f)
                    val tone = sin(2f * PI.toFloat() * 450f * (i.toFloat() / sampleRate)) * 0.7f
                    val noise = (rand.nextFloat() * 2f - 1f) * 0.3f
                    buffer[i] = ((tone + noise) * env * 22000f).toInt().toShort()
                }
            }
            BlockSoundType.WOOD -> {
                // Hollow resonant thud
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = exp(-t * 6f)
                    val tone = sin(2f * PI.toFloat() * 180f * (i.toFloat() / sampleRate)) * 0.8f
                    buffer[i] = (tone * env * 20000f).toInt().toShort()
                }
            }
            BlockSoundType.SAND -> {
                // Grainy swish
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = (1f - t)
                    val noise = (rand.nextFloat() * 2f - 1f) * 0.5f
                    buffer[i] = (noise * env * 18000f).toInt().toShort()
                }
            }
            BlockSoundType.WATER -> {
                // Liquid splash
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = exp(-t * 4f)
                    val freq = 400f - t * 250f
                    val tone = sin(2f * PI.toFloat() * freq * (i.toFloat() / sampleRate)) * 0.6f
                    val noise = (rand.nextFloat() * 2f - 1f) * 0.4f
                    buffer[i] = ((tone + noise) * env * 20000f).toInt().toShort()
                }
            }
            else -> {
                // Generic tap
                for (i in 0 until numSamples) {
                    val t = i.toFloat() / numSamples
                    val env = exp(-t * 8f)
                    val noise = (rand.nextFloat() * 2f - 1f) * 0.5f
                    buffer[i] = (noise * env * 18000f).toInt().toShort()
                }
            }
        }

        playPcm(buffer, sfxVolume)
    }

    fun playBlockHit() {
        val numSamples = (sampleRate * 60) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = exp(-t * 10f)
            val noise = (rand.nextFloat() * 2f - 1f) * 0.6f
            val click = sin(2f * PI.toFloat() * 320f * (i.toFloat() / sampleRate)) * 0.4f
            buffer[i] = ((noise + click) * env * 18000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.7f)
    }

    fun playBlockBreak() {
        val numSamples = (sampleRate * 180) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = (1f - t) * (1f - t)
            val noise = (rand.nextFloat() * 2f - 1f) * 0.7f
            val pitch = 220f - t * 80f
            val crunch = sin(2f * PI.toFloat() * pitch * (i.toFloat() / sampleRate)) * 0.3f
            buffer[i] = ((noise + crunch) * env * 24000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume)
    }

    fun playBlockPlace() {
        val numSamples = (sampleRate * 90) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = exp(-t * 8f)
            val thud = sin(2f * PI.toFloat() * 150f * (i.toFloat() / sampleRate)) * 0.8f
            buffer[i] = (thud * env * 22000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume)
    }

    fun playWaterSplash() {
        val numSamples = (sampleRate * 250) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = exp(-t * 6f)
            val noise = (rand.nextFloat() * 2f - 1f) * 0.7f
            val bubble = sin(2f * PI.toFloat() * (450f - t * 200f) * (i.toFloat() / sampleRate)) * 0.5f
            buffer[i] = ((noise + bubble) * env * 22000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.85f)
    }

    fun playCreatureSound(isHostile: Boolean) {
        val durationMs = if (isHostile) 350 else 220
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val baseFreq = if (isHostile) 95f else 280f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = sin(t * Math.PI.toFloat())
            val freqMod = baseFreq + sin(t * 20f) * (if (isHostile) 35f else 70f)
            val tone = sin(2f * PI.toFloat() * freqMod * (i.toFloat() / sampleRate))
            val grit = (rand.nextFloat() * 2f - 1f) * (if (isHostile) 0.45f else 0.15f)
            buffer[i] = ((tone * 0.75f + grit) * env * 18000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.8f)
    }

    fun playRainSound() {
        val numSamples = (sampleRate * 400) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = sin(t * Math.PI.toFloat())
            val noise = (rand.nextFloat() * 2f - 1f) * 0.55f
            buffer[i] = (noise * env * 12000f).toInt().toShort()
        }
        playPcm(buffer, ambientVolume * 0.65f)
    }

    fun playWindAmbience() {
        val numSamples = (sampleRate * 600) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = sin(t * Math.PI.toFloat())
            val wave = sin(2f * PI.toFloat() * (120f + sin(t * 4f) * 30f) * (i.toFloat() / sampleRate)) * 0.4f
            val whisper = (rand.nextFloat() * 2f - 1f) * 0.3f
            buffer[i] = ((wave + whisper) * env * 14000f).toInt().toShort()
        }
        playPcm(buffer, ambientVolume * 0.6f)
    }

    fun playUiClick() {
        val numSamples = (sampleRate * 40) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = exp(-t * 12f)
            val tone = sin(2f * PI.toFloat() * 750f * (i.toFloat() / sampleRate))
            buffer[i] = (tone * env * 16000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.6f)
    }

    fun playTorchPlace() {
        val numSamples = (sampleRate * 70) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = exp(-t * 9f)
            val swoosh = sin(2f * PI.toFloat() * (280f + t * 180f) * (i.toFloat() / sampleRate)) * 0.5f
            val hiss = (rand.nextFloat() * 2f - 1f) * 0.35f
            buffer[i] = ((swoosh + hiss) * env * 18000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.75f)
    }

    fun playHurt() {
        val numSamples = (sampleRate * 140) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = (1f - t) * exp(-t * 5f)
            val groan = sin(2f * PI.toFloat() * (160f - t * 40f) * (i.toFloat() / sampleRate)) * 0.8f
            buffer[i] = (groan * env * 24000f).toInt().toShort()
        }
        playPcm(buffer, sfxVolume * 0.9f)
    }

    fun playLevelUp() {
        scope.launch(Dispatchers.Default) {
            val notes = listOf(392f, 523.25f, 659.25f, 783.99f)
            for (freq in notes) {
                playChimeNote(freq)
                delay(120)
            }
        }
    }

    private fun startAmbientMusicLoop() {
        musicJob = scope.launch(Dispatchers.Default) {
            // Pentatonic scale frequencies in Hz: C4 (261.63), D4 (293.66), E4 (329.63), G4 (392.00), A4 (440.00), C5 (523.25)
            val pentatonicScale = floatArrayOf(261.63f, 293.66f, 329.63f, 392.00f, 440.00f, 523.25f, 587.33f)

            while (isActive) {
                // Play a peaceful sequence of 4 to 6 gentle ambient bell chimes
                val notesInPhrase = 4 + rand.nextInt(3)
                for (n in 0 until notesInPhrase) {
                    if (!isActive) break
                    val freq = pentatonicScale[rand.nextInt(pentatonicScale.size)]
                    playChimeNote(freq)
                    delay(700L + rand.nextInt(800))
                }

                // Long peaceful pause between ambient musical phrases (20 to 35 seconds)
                delay(22000L + rand.nextInt(15000))
            }
        }
    }

    private fun playChimeNote(frequency: Float) {
        val durationMs = 1200
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 2.5f)
            // Fundamental + octave overtone + fifth overtone
            val fundamental = sin(2f * PI.toFloat() * frequency * t) * 0.6f
            val harmonic1 = sin(2f * PI.toFloat() * frequency * 2f * t) * 0.25f
            val harmonic2 = sin(2f * PI.toFloat() * frequency * 3f * t) * 0.15f
            buffer[i] = ((fundamental + harmonic1 + harmonic2) * env * 14000f).toInt().toShort()
        }

        playPcm(buffer, musicVolume)
    }
}
