package com.novelreader.data.parser

import java.io.InputStream
import java.util.zip.ZipInputStream

object ZipParser {

    /** 单个文件最大 50MB，避免 OOM */
    private const val MAX_ENTRY_SIZE = 50 * 1024 * 1024

    data class ZipEntry(
        val name: String,
        val inputStreamProvider: () -> InputStream
    )

    /**
     * 扫描 zip 文件，返回所有支持的书籍文件（txt/epub）。
     * 使用懒加载 InputStream 避免一次性全部加载到内存。
     */
    fun scan(inputStream: InputStream): List<ZipEntry> {
        val entries = mutableListOf<ZipEntry>()
        val data = mutableMapOf<String, ByteArray>()
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val name = entry.name
                val lowerName = name.lowercase()
                if (lowerName.endsWith(".txt") || lowerName.endsWith(".epub")) {
                    // 跳过 macOS 元数据和隐藏文件
                    if (!name.startsWith("__MACOSX") && !name.substringAfterLast('/').startsWith(".")) {
                        // 限制单文件大小，跳过超大文件
                        val maxSize = if (entry.size > 0) minOf(entry.size, MAX_ENTRY_SIZE.toLong()) else MAX_ENTRY_SIZE.toLong()
                        val bytes = zis.readBytes()
                        if (bytes.size <= MAX_ENTRY_SIZE) {
                            data[name] = bytes
                        }
                    }
                }
            }
            entry = zis.nextEntry
        }
        zis.close()

        for ((name, bytes) in data) {
            entries.add(ZipEntry(
                name = name.substringAfterLast('/'),
                inputStreamProvider = { bytes.inputStream() }
            ))
        }
        return entries
    }
}
