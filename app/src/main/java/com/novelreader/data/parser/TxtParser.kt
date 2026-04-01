package com.novelreader.data.parser

import com.novelreader.data.model.Chapter
import java.io.InputStream
import java.nio.charset.Charset

object TxtParser {

    val chapterPatterns = listOf(
        Regex("""^第[0-9零一二三四五六七八九十百千]+[章节回集篇].*"""),
        Regex("""^Chapter\s+\d+.*""", RegexOption.IGNORE_CASE),
        Regex("""^【.+?】.*"""),
        Regex("""^\d+[.\s、].{2,30}$"""),
    )

    fun parse(inputStream: InputStream, charset: Charset = Charsets.UTF_8): List<Chapter> {
        val text = String(inputStream.readBytes(), charset)
        return parse(text)
    }

    fun parse(text: String): List<Chapter> {
        val lines = text.lines()
        val chapters = mutableListOf<Chapter>()
        val currentContent = StringBuilder()
        var currentTitle: String? = null
        var chapterIndex = 0

        fun flushChapter() {
            val content = currentContent.toString().trim()
            if (content.isNotEmpty() || currentTitle != null) {
                chapters.add(
                    Chapter(
                        title = currentTitle ?: "未命名章节",
                        content = content,
                        index = chapterIndex++
                    )
                )
            }
            currentContent.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (isChapterTitle(trimmed)) {
                flushChapter()
                currentTitle = trimmed
            } else {
                if (currentContent.isNotEmpty()) {
                    currentContent.append("\n")
                }
                currentContent.append(line)
            }
        }
        flushChapter()

        // If no chapters found, split by large empty gaps
        if (chapters.isEmpty() || (chapters.size == 1 && chapters[0].title == "未命名章节")) {
            return splitByEmptyLines(text)
        }

        return chapters
    }

    private fun isChapterTitle(line: String): Boolean {
        if (line.length > 50) return false
        return chapterPatterns.any { it.matches(line) }
    }

    private fun splitByEmptyLines(text: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val paragraphs = text.split(Regex("""\n\s*\n"""))
        var index = 0
        for (para in paragraphs) {
            val trimmed = para.trim()
            if (trimmed.isNotEmpty()) {
                val title = trimmed.take(30).replace("\n", " ")
                chapters.add(
                    Chapter(
                        title = "段落 ${++index}",
                        content = trimmed,
                        index = index
                    )
                )
            }
        }
        if (chapters.isEmpty()) {
            chapters.add(Chapter(title = "全文", content = text, index = 0))
        }
        return chapters
    }
}
