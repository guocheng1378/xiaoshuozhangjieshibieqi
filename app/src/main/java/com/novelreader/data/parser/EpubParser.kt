package com.novelreader.data.parser

import com.novelreader.data.model.Chapter
import java.io.InputStream
import java.util.zip.ZipInputStream

object EpubParser {

    fun parse(inputStream: InputStream): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val zipInputStream = ZipInputStream(inputStream)
        val htmlContents = mutableListOf<Pair<String, String>>() // href -> content

        // Collect all files in the ZIP
        val entries = mutableMapOf<String, ByteArray>()
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val name = entry.name
                val bytes = zipInputStream.readBytes()
                entries[name] = bytes
            }
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()

        // Find OPF file
        val containerXml = entries["META-INF/container.xml"]?.toString(Charsets.UTF_8) ?: return chapters
        val opfPath = Regex("""<rootfile\s+[^>]*full-path="([^"]+)"""")
            .find(containerXml)?.groupValues?.get(1) ?: return chapters

        val opfDir = opfPath.substringBeforeLast("/", "")
        val opfContent = entries[opfPath]?.toString(Charsets.UTF_8) ?: return chapters

        // Parse spine order from OPF
        val manifest = mutableMapOf<String, String>() // id -> href
        val manifestRegex = Regex("""<item\s+[^>]*id="([^"]+)"[^>]*href="([^"]+)"[^>]*/?>""")
        for (match in manifestRegex.findAll(opfContent)) {
            val id = match.groupValues[1]
            val href = match.groupValues[2]
            manifest[id] = href
        }

        val spineItemRefs = Regex("""<itemref\s+[^>]*idref="([^"]+)"[^>]*/?>""")
            .findAll(opfContent)
            .map { it.groupValues[1] }
            .toList()

        // Read chapters in spine order
        var chapterIndex = 0
        for (itemRef in spineItemRefs) {
            val href = manifest[itemRef] ?: continue
            val fullPath = if (opfDir.isNotEmpty()) "$opfDir/$href" else href
            val htmlBytes = entries[fullPath] ?: continue
            val html = htmlBytes.toString(Charsets.UTF_8)

            val title = extractTitle(html) ?: "章节 ${chapterIndex + 1}"
            val content = stripHtml(html).trim()

            if (content.isNotEmpty()) {
                chapters.add(
                    Chapter(
                        title = title,
                        content = content,
                        index = chapterIndex++
                    )
                )
            }
        }

        return chapters
    }

    private fun extractTitle(html: String): String? {
        // Try <title> tag
        val titleMatch = Regex("""<title[^>]*>(.*?)</title>""", RegexOption.IGNORE_CASE)
            .find(html)
        if (titleMatch != null) {
            val title = stripHtml(titleMatch.groupValues[1]).trim()
            if (title.isNotEmpty()) return title
        }
        // Try <h1> to <h3>
        for (level in 1..3) {
            val hMatch = Regex("""<h$level[^>]*>(.*?)</h$level>""", RegexOption.IGNORE_CASE)
                .find(html)
            if (hMatch != null) {
                val heading = stripHtml(hMatch.groupValues[1]).trim()
                if (heading.isNotEmpty()) return heading
            }
        }
        return null
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<p[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</p>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}
