package com.novelreader.util

import java.io.InputStream
import java.nio.charset.Charset

object EncodingDetector {

    fun detectAndRead(inputStream: InputStream, forcedCharset: String? = null): Pair<String, Charset> {
        val bytes = inputStream.readBytes()

        // 强制编码优先
        if (forcedCharset != null) {
            val charset = Charset.forName(forcedCharset)
            return String(bytes, charset) to charset
        }

        // BOM 检测
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8) to Charsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE) to Charsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE) to Charsets.UTF_16LE
        }

        // UTF-8 验证
        try {
            val text = String(bytes, Charsets.UTF_8)
            if (text.toByteArray(Charsets.UTF_8).contentEquals(bytes)) {
                return text to Charsets.UTF_8
            }
        } catch (_: Exception) {}

        // GBK 回退
        return String(bytes, Charset.forName("GBK")) to Charset.forName("GBK")
    }
}
