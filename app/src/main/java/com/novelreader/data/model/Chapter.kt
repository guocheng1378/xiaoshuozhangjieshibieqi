package com.novelreader.data.model

data class Chapter(
    val title: String,
    val content: String,
    val index: Int
) {
    val preview: String
        get() = content.take(150).replace("\n", " ").trim() + if (content.length > 150) "..." else ""

    val wordCount: Int
        get() = content.replace("\\s".toRegex(), "").length
}
