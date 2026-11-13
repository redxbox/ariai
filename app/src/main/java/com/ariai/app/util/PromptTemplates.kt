package com.ariai.app.util

/** A starting text for a common request. [title] is a UI key; the text follows the UI language. */
data class PromptTemplate(val title: String, val fa: String, val en: String) {
    fun text(lang: String): String = if (lang == "fa") fa else en
}

val promptTemplates: List<PromptTemplate> = listOf(
    PromptTemplate(
        "Summarize",
        "این متن را در چند نکته‌ی کوتاه خلاصه کن:",
        "Summarize the following text in a few short points:"
    ),
    PromptTemplate(
        "Translate to Persian",
        "این متن را به فارسی روان ترجمه کن:",
        "Translate the following text into natural Persian:"
    ),
    PromptTemplate(
        "Explain simply",
        "این موضوع را ساده و با یک مثال توضیح بده:",
        "Explain this simply and give one example:"
    ),
    PromptTemplate(
        "Improve writing",
        "نوشتار این متن را بهتر کن و معنی آن را حفظ کن:",
        "Improve the writing of this text and keep its meaning:"
    ),
    PromptTemplate(
        "Code review",
        "این کد را از نظر خطا و ریسک بررسی کن و اصلاح پیشنهاد بده:",
        "Review this code for bugs and risks and suggest fixes:"
    )
)
