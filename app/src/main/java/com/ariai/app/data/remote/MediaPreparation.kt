package com.ariai.app.data.remote

import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Base64
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/** Samples per second for audio sent to OpenAI-style endpoints. Speech stays intelligible at 16 kHz. */
internal const val AUDIO_TARGET_RATE = 16_000
/** Longest audio clip converted for a request; longer clips are cut and the rest is not sent. */
internal const val AUDIO_MAX_SECONDS = 180
/** Frames sampled from a video for models that accept images but not video. */
internal const val VIDEO_FRAME_COUNT = 4
private const val FRAME_MAX_EDGE = 768

/**
 * Prepares the messages for one request, only when the selected model needs it:
 * - vision models without video input receive still frames taken from each video;
 * - OpenAI-style endpoints receive audio as WAV when the clip is not already wav or mp3.
 * Stored messages are never changed; this works on the copies sent in the request.
 */
internal suspend fun prepareMessagesForModel(
    provider: Provider,
    modelId: String,
    messages: List<ChatMessage>
): List<ChatMessage> = withContext(Dispatchers.IO) {
    val model = provider.models.firstOrNull { it.id == modelId }
    val media = mediaInputFor(provider, modelId)
    val openAiStyle = provider.type != ProviderType.GEMINI && provider.type != ProviderType.ANTHROPIC
    val framesForVideo = !media.video && model?.supportsVision == true
    val convertAudio = openAiStyle && media.audio
    if (!framesForVideo && !convertAudio) return@withContext messages

    messages.map { message ->
        val needsWork = message.attachments.any { attachment ->
            (attachment.type == AttachmentType.VIDEO && framesForVideo) ||
                (attachment.type == AttachmentType.AUDIO && convertAudio && openAiAudioFormat(attachment.mimeType) == null)
        }
        if (!needsWork) return@map message
        message.copy(
            attachments = message.attachments.flatMap { attachment ->
                when {
                    attachment.type == AttachmentType.VIDEO && framesForVideo ->
                        framesFor(attachment).ifEmpty { listOf(attachment) }
                    attachment.type == AttachmentType.AUDIO && convertAudio && openAiAudioFormat(attachment.mimeType) == null ->
                        listOf(wavFor(attachment) ?: attachment)
                    else -> listOf(attachment)
                }
            }
        )
    }
}

// Small cache so the same attachment is not decoded again on every request in the same chat.
private val preparedCache = object : LinkedHashMap<String, List<Attachment>>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Attachment>>?): Boolean = size > 12
}

private fun cached(key: String, produce: () -> List<Attachment>): List<Attachment> {
    synchronized(preparedCache) { preparedCache[key] }?.let { return it }
    val produced = produce()
    synchronized(preparedCache) { preparedCache[key] = produced }
    return produced
}

private fun framesFor(video: Attachment): List<Attachment> = cached("frames:${video.id}") {
    val bytes = runCatching { Base64.decode(video.base64Data.orEmpty(), Base64.DEFAULT) }.getOrNull()
        ?: return@cached emptyList()
    extractVideoFrames(bytes, video.name, VIDEO_FRAME_COUNT)
}

private fun wavFor(audio: Attachment): Attachment? = cached("wav:${audio.id}") {
    val bytes = runCatching { Base64.decode(audio.base64Data.orEmpty(), Base64.DEFAULT) }.getOrNull()
        ?: return@cached emptyList()
    val clip = decodeToMonoPcm(bytes) ?: return@cached emptyList()
    listOf(
        audio.copy(
            mimeType = "audio/wav",
            base64Data = Base64.encodeToString(wavBytes(clip.pcm, clip.sampleRate), Base64.NO_WRAP)
        )
    )
}.firstOrNull()

/** Evenly spaced still frames from the video, scaled down and JPEG-encoded. Empty when nothing can be read. */
internal fun extractVideoFrames(videoBytes: ByteArray, name: String, frameCount: Int): List<Attachment> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(ByteArrayMediaDataSource(videoBytes))
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: return emptyList()
        if (durationMs <= 0 || frameCount <= 0) return emptyList()
        buildList {
            for (index in 0 until frameCount) {
                // Centre of each equal slice of the video.
                val timeUs = durationMs * 1000L * (2 * index + 1) / (2 * frameCount)
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: continue
                val scaled = scaleToMaxEdge(frame, FRAME_MAX_EDGE)
                val jpeg = ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                if (scaled !== frame) scaled.recycle()
                frame.recycle()
                add(
                    Attachment(
                        type = AttachmentType.IMAGE,
                        name = "$name (frame ${index + 1})",
                        mimeType = "image/jpeg",
                        base64Data = Base64.encodeToString(jpeg.toByteArray(), Base64.NO_WRAP)
                    )
                )
            }
        }
    } catch (failure: Exception) {
        com.ariai.app.util.AppLog.warn(failure, "extract video frames")
        emptyList()
    } finally {
        retriever.release()
    }
}

private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).roundToInt().coerceAtLeast(1),
        (bitmap.height * scale).roundToInt().coerceAtLeast(1),
        true
    )
}

internal class DecodedPcm(val pcm: ByteArray, val sampleRate: Int)

/** Decodes any audio track the device can play to 16-bit mono PCM at up to [AUDIO_TARGET_RATE]. */
internal fun decodeToMonoPcm(audioBytes: ByteArray, maxSeconds: Int = AUDIO_MAX_SECONDS): DecodedPcm? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    return try {
        extractor.setDataSource(ByteArrayMediaDataSource(audioBytes))
        var trackIndex = -1
        var trackFormat: MediaFormat? = null
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = index
                trackFormat = format
                break
            }
        }
        val inputFormat = trackFormat ?: return null
        extractor.selectTrack(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
        inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        val decoder = MediaCodec.createDecoderByType(mime)
        codec = decoder
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        var resampler: MonoResampler? = null
        var channels = 1
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inputIndex) ?: return null
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = decoder.outputFormat
                    val sourceRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    resampler = MonoResampler(sourceRate, maxSeconds.toLong() * AUDIO_TARGET_RATE)
                }
                else -> if (outputIndex >= 0) {
                    val output = decoder.getOutputBuffer(outputIndex)
                    val target = resampler
                    if (output != null && target != null && info.size > 0) {
                        output.position(info.offset)
                        output.limit(info.offset + info.size)
                        val shorts = output.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (shorts.remaining() >= channels && !target.isFull) {
                            var sum = 0
                            repeat(channels) { sum += shorts.get().toInt() }
                            target.push(sum / channels)
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || resampler?.isFull == true) {
                        outputDone = true
                    }
                }
            }
        }
        val finished = resampler ?: return null
        DecodedPcm(finished.finish(), finished.targetRate)
    } catch (failure: Exception) {
        com.ariai.app.util.AppLog.warn(failure, "decode audio for model")
        null
    } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        extractor.release()
    }
}

/** Box-filter downsampling of mono 16-bit samples to a lower rate, with a sample cap. */
internal class MonoResampler(sourceRate: Int, private val maxSamples: Long) {
    val targetRate: Int = minOf(AUDIO_TARGET_RATE, sourceRate)
    private val ratio = sourceRate.toDouble() / targetRate
    private var seen = 0L
    private var emitted = 0L
    private var sum = 0L
    private var count = 0
    private val out = ByteArrayOutputStream()

    val isFull: Boolean get() = emitted >= maxSamples

    fun push(sample: Int) {
        if (isFull) return
        sum += sample
        count++
        seen++
        if (seen >= (emitted + 1) * ratio) {
            writeSample((sum / count).toInt())
            emitted++
            sum = 0
            count = 0
        }
    }

    fun finish(): ByteArray {
        if (count > 0 && !isFull) {
            writeSample((sum / count).toInt())
            emitted++
            sum = 0
            count = 0
        }
        return out.toByteArray()
    }

    private fun writeSample(sample: Int) {
        out.write(sample and 0xff)
        out.write((sample shr 8) and 0xff)
    }
}

/** A complete WAV file (RIFF, PCM) around 16-bit little-endian samples. */
internal fun wavBytes(pcm: ByteArray, sampleRate: Int, channels: Int = 1): ByteArray {
    val bitsPerSample = 16
    val blockAlign = channels * bitsPerSample / 8
    return ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt(36 + pcm.size)
        put("WAVE".toByteArray(Charsets.US_ASCII))
        put("fmt ".toByteArray(Charsets.US_ASCII))
        putInt(16)
        putShort(1)
        putShort(channels.toShort())
        putInt(sampleRate)
        putInt(sampleRate * blockAlign)
        putShort(blockAlign.toShort())
        putShort(bitsPerSample.toShort())
        put("data".toByteArray(Charsets.US_ASCII))
        putInt(pcm.size)
        put(pcm)
    }.array()
}

private class ByteArrayMediaDataSource(private val bytes: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= bytes.size) return -1
        val count = minOf(size.toLong(), bytes.size - position).toInt()
        System.arraycopy(bytes, position.toInt(), buffer, offset, count)
        return count
    }

    override fun getSize(): Long = bytes.size.toLong()

    override fun close() = Unit
}
