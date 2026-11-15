package com.ariai.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MediaPreparationTest {
    @Test
    fun wavHeaderDescribesMonoSixteenBitPcm() {
        val pcm = ByteArray(8) { it.toByte() }
        val wav = wavBytes(pcm, sampleRate = 16_000)
        val header = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals(44 + pcm.size, wav.size)
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals(36 + pcm.size, header.getInt(4))
        assertEquals(1, header.getShort(22).toInt())   // channels
        assertEquals(16_000, header.getInt(24))        // sample rate
        assertEquals(32_000, header.getInt(28))        // byte rate
        assertEquals(16, header.getShort(34).toInt())  // bits per sample
        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
        assertEquals(pcm.size, header.getInt(40))
        assertTrue(pcm.contentEquals(wav.copyOfRange(44, wav.size)))
    }

    @Test
    fun resamplerDownsamplesToSixteenKilohertzWithAverage() {
        val resampler = MonoResampler(sourceRate = 48_000, maxSamples = 10_000)
        repeat(4_800) { resampler.push(1_000) }
        val out = resampler.finish()

        assertEquals(16_000, resampler.targetRate)
        assertEquals(1_600 * 2, out.size)
        for (i in 0 until out.size step 2) {
            assertEquals(1_000, (out[i].toInt() and 0xff) or (out[i + 1].toInt() shl 8))
        }
    }

    @Test
    fun resamplerKeepsLowRatesAndStopsAtTheSampleCap() {
        val low = MonoResampler(sourceRate = 8_000, maxSamples = 100)
        assertEquals(8_000, low.targetRate)

        val capped = MonoResampler(sourceRate = 16_000, maxSamples = 10)
        repeat(100) { capped.push(7) }
        assertTrue(capped.isFull)
        assertEquals(20, capped.finish().size)
    }
}
