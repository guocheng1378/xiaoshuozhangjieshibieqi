package com.novelreader.data.parser

import com.novelreader.data.model.Chapter
import java.io.InputStream
import java.nio.charset.Charset

object TxtParser {

    val chapterPatterns = listOf(
        // Markdown headers: ## 第X章
        Regex("""^#{1,3}\s*第[零一二三四五六七八九十百千0-9]+[章节回集篇].{0,40}$"""),
        // Plain: 第X章 标题（允许空格或标点后接标题）
        Regex("""^第[零一二三四五六七八九十百千0-9]+[章节回集篇][\s:：、.．].{0,40}$"""),
        // 第X章标题（无空格）
        Regex("""^第[零一二三四五六七八九十百千0-9]+[章节回集篇][^\s【】]{1,30}$"""),
        // Chapter X
        Regex("""^Chapter\s+\d+.{0,40}$""", RegexOption.IGNORE_CASE),
        // 【第X章 标题】—— 仅匹配含章节编号的方括号标题（排除"完""未完"）
        Regex("""^【第[零一二三四五六七八九十百千0-9]+[章节回集卷篇][\s:：][^】完未]{1,40}】$"""),
        // Volume markers: 第X卷：标题（需有标点分隔）
        Regex("""^第[零一二三四五六七八九十百千0-9]+[卷篇部][\s:：、.．].{2,40}$"""),
        // Markdown volume: # 卷/篇/部
        Regex("""^#{1,3}\s*(卷|篇|部).{0,40}$"""),
        Regex("""^(卷|篇|部)\s*[第0-9].{0,40}$"""),
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
                        title = cleanTitle(currentTitle ?: "未命名章节"),
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
                // Skip horizontal rules and empty volume markers
                if (trimmed.matches(Regex("""^-{3,}$""")) || trimmed.matches(Regex("""^\*{3,}$"""))) {
                    continue
                }
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

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""^#{1,3}\s*"""), "")  // Remove markdown #
            .replace(Regex("""^-{3,}$"""), "")      // Remove ---
            .trim()
    }

    private fun isChapterTitle(line: String): Boolean {
        if (line.length > 60) return false
        if (line.isBlank()) return false
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
