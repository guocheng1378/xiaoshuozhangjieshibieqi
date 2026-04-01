package com.novelreader.data.model

data class BookFile(
    val filePath: String,
    val fileName: String,
    val lastChapterIndex: Int = 0,
    val lastScrollOffset: Int = 0
)
