package com.ariai.app.data.remote

import com.ariai.app.data.models.AIModel
import com.ariai.app.data.models.Attachment
import com.ariai.app.data.models.AttachmentType
import com.ariai.app.data.models.ChatMessage
import com.ariai.app.data.models.MessageRole
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaInputCapabilityTest {
    private fun provider(type: ProviderType, vararg models: AIModel) =
        Provider(name = "p", type = type, baseUrl = "https://example.test/v1", models = models.toList())

    private fun model(id: String, audio: Boolean = false, video: Boolean = false) =
        AIModel(id = id, displayName = id, providerId = "p", supportsAudioInput = audio, supportsVideoInput = video)

    @Test
    fun audioInputComesOnlyFromTheModelItself() {
        val p = provider(ProviderType.OPENAI_COMPATIBLE, model("listens", audio = true), model("silent"))
        assertTrue(mediaInputFor(p, "listens").audio)
        assertFalse(mediaInputFor(p, "silent").audio)
        assertFalse(mediaInputFor(p, "unknown").audio)
    }

    @Test
    fun geminiVideoDefaultAppliesOnlyToRowsWithoutModelData() {
        val gemini = provider(ProviderType.GEMINI, model("old"))
        assertTrue(mediaInputFor(gemini, "missing-row").video)
        assertFalse(mediaInputFor(gemini, "old").video)
    }

    @Test
    fun openAiAudioFormatAcceptsOnlyWavAndMp3() {
        assertEquals("wav", openAiAudioFormat("audio/wav"))
        assertEquals("mp3", openAiAudioFormat("audio/mpeg"))
        assertNull(openAiAudioFormat("audio/ogg"))
    }

    @Test
    fun unsentAudioGetsANoteAndSentAudioDoesNot() {
        val clip = Attachment(type = AttachmentType.AUDIO, name = "a.mp3", mimeType = "audio/mpeg", base64Data = "AAA")
        val message = ChatMessage(chatId = "c", role = MessageRole.USER, content = "hi", attachments = listOf(clip))
        assertTrue(message.promptText().contains("[Audio attached: a.mp3. This model cannot listen to audio.]"))
        assertFalse(message.promptText(sendAudio = { true }).contains("cannot listen"))
    }
}
