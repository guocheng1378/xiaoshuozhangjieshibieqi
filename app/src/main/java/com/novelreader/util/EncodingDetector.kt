package com.novelreader.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset

object EncodingDetector {

    fun detectAndRead(inputStream: InputStream): Pair<String, Charset> {
        val bytes = inputStream.readBytes()
        // Do NOT close the stream here — caller may still need it

        // BOM detection
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charset.forName("UTF-8")) to Charset.forName("UTF-8")
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE")) to Charset.forName("UTF-16BE")
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE")) to Charset.forName("UTF-16LE")
        }

        // Try UTF-8
        try {
            val text = String(bytes, Charset.forName("UTF-8"))
            if (text.toByteArray(Charset.forName("UTF-8")).contentEquals(bytes)) {
                return text to Charset.forName("UTF-8")
            }
        } catch (_: Exception) {}

        // Fallback to GBK
        return String(bytes, Charset.forName("GBK")) to Charset.forName("GBK")
    }

    fun detectAndRead(inputStream: InputStream, forcedCharset: String?): Pair<String, Charset> {
        val bytes = inputStream.readBytes()
        if (forcedCharset != null) {
            val charset = Charset.forName(forcedCharset)
            return String(bytes, charset) to charset
        }

        // BOM detection
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charset.forName("UTF-8")) to Charset.forName("UTF-8")
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE")) to Charset.forName("UTF-16BE")
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE")) to Charset.forName("UTF-16LE")
        }

        // Try UTF-8
        try {
            val text = String(bytes, Charset.forName("UTF-8"))
            if (text.toByteArray(Charset.forName("UTF-8")).contentEquals(bytes)) {
                return text to Charset.forName("UTF-8")
            }
        } catch (_: Exception) {}

        // Fallback to GBK
        return String(bytes, Charset.forName("GBK")) to Charset.forName("GBK")
    }
}
