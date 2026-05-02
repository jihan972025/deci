package com.deci

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

    // Statistics tracking
    private val recentValues = mutableListOf<Float>()
    private val maxRecentValues = 100
    private val leqValues = mutableListOf<Double>()
    private var leqStartTime = 0L

    var minDb = Float.MAX_VALUE
        private set
    var maxDb = 0f
        private set
    var avgDb = 0f
        private set
    var currentLeq = 0f
        private set

    fun start() {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            audioRecord?.startRecording()
            isRecording = true
            leqStartTime = System.currentTimeMillis()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isRecording = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        recentValues.clear()
        leqValues.clear()
        minDb = Float.MAX_VALUE
        maxDb = 0f
        avgDb = 0f
        currentLeq = 0f
        leqStartTime = 0L
    }

    fun resetStatistics() {
        recentValues.clear()
        leqValues.clear()
        minDb = Float.MAX_VALUE
        maxDb = 0f
        avgDb = 0f
        currentLeq = 0f
        leqStartTime = System.currentTimeMillis()
    }

    suspend fun getDecibel(): Pair<Float, Float> = withContext(Dispatchers.IO) {
        if (!isRecording || audioRecord == null) {
            return@withContext Pair(0f, 0f)
        }

        val buffer = ShortArray(bufferSize)
        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0

        if (read > 0) {
            // Calculate RMS
            var sum = 0.0
            for (i in 0 until read) {
                sum += buffer[i] * buffer[i]
            }
            val rms = sqrt(sum / read)

            // Convert to decibels (A-weighted approximation)
            val amplitude = rms / 32768.0
            val db = if (amplitude > 0) {
                // Reference: 94 dB SPL = 1 Pa = full scale
                // Calibration offset to match typical SPL meters
                20 * log10(amplitude) + 94.0
            } else {
                0.0
            }.toFloat().coerceIn(0f, 120f)

            // Update statistics
            updateStatistics(db)

            // Return db and amplitude for spectrum
            Pair(db, rms.toFloat())
        } else {
            Pair(0f, 0f)
        }
    }

    private fun updateStatistics(db: Float) {
        recentValues.add(db)
        if (recentValues.size > maxRecentValues) {
            recentValues.removeAt(0)
        }

        if (db > 0) {
            minDb = minOf(minDb, db)
            maxDb = maxOf(maxDb, db)
        }

        avgDb = if (recentValues.isNotEmpty()) {
            recentValues.average().toFloat()
        } else {
            0f
        }

        // Update LEQ (Equivalent Continuous Sound Level)
        updateLeq(db)
    }

    private fun updateLeq(db: Float) {
        // LEQ = 10 * log10(1/T * Σ(10^(Li/10)))
        // Convert dB to linear pressure
        val linearPressure = Math.pow(10.0, db / 10.0)
        leqValues.add(linearPressure)

        // Keep only last 1000 samples (about 2 minutes at 125ms intervals)
        if (leqValues.size > 1000) {
            leqValues.removeAt(0)
        }

        // Calculate LEQ
        if (leqValues.isNotEmpty()) {
            val avgLinear = leqValues.average()
            currentLeq = (10.0 * log10(avgLinear)).toFloat().coerceIn(0f, 120f)
        }
    }

    fun getL90(): Float {
        // L90: Sound level exceeded 90% of the time (baseline)
        if (recentValues.size < 10) return 0f
        val sorted = recentValues.sorted()
        val index = (sorted.size * 0.9).toInt()
        return sorted.getOrNull(index) ?: 0f
    }

    fun getL10(): Float {
        // L10: Sound level exceeded 10% of the time (peak)
        if (recentValues.size < 10) return 0f
        val sorted = recentValues.sorted()
        val index = (sorted.size * 0.1).toInt()
        return sorted.getOrNull(index) ?: 0f
    }

    fun isActive(): Boolean = isRecording
}
