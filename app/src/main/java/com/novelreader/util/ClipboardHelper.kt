package com.novelreader.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardHelper {

    fun copyText(context: Context, text: String, label: String = "novel_text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        HapticHelper.trigger(context)
    }

    fun copyTitle(context: Context, title: String) {
        copyText(context, title, "chapter_title")
        Toast.makeText(context, "标题已复制", Toast.LENGTH_SHORT).show()
    }

    fun copyContent(context: Context, content: String, wordCount: Int) {
        copyText(context, content, "chapter_content")
        Toast.makeText(context, "内容已复制（约${wordCount}字）", Toast.LENGTH_SHORT).show()
    }
}
