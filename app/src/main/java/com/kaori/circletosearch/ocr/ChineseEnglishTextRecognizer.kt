/*
 * Copyright (C) 2025 Kaori
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.kaori.circletosearch.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.kaori.circletosearch.ui.components.TextNode
import com.kaori.circletosearch.ui.components.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Offline OCR for screenshots containing Simplified Chinese, English, or Japanese. */
object ChineseEnglishTextRecognizer {
    suspend fun extractText(bitmap: Bitmap): List<TextNode> = withContext(Dispatchers.Default) {
        val chineseRecognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
        val japaneseRecognizer = TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val chineseResult = chineseRecognizer.process(image).awaitResult()
            val japaneseResult = japaneseRecognizer.process(image).awaitResult()
            selectBestResult(chineseResult, japaneseResult)
                .textBlocks
                .flatMap { block -> block.lines }
                .mapNotNull { line -> line.toTextNodeOrNull() }
        } finally {
            chineseRecognizer.close()
            japaneseRecognizer.close()
        }
    }

    /**
     * Kana is unambiguous evidence for Japanese. When it is absent, retain the
     * established Chinese recognizer result so Chinese-only screenshots keep
     * their prior behavior; Japanese kanji are shared with the Chinese model.
     */
    private fun selectBestResult(chinese: Text, japanese: Text): Text {
        val japaneseText = japanese.text
        return when {
            chinese.text.isBlank() -> japanese
            japaneseText.any(::isJapaneseKana) -> japanese
            else -> chinese
        }
    }

    private fun isJapaneseKana(character: Char): Boolean =
        character in '\u3040'..'\u309f' || character in '\u30a0'..'\u30ff' || character in '\uff66'..'\uff9f'

    private fun Text.Line.toTextNodeOrNull(): TextNode? {
        val lineBounds = boundingBox ?: return null
        val words = elements.mapIndexedNotNull { index, element ->
            val bounds = element.boundingBox ?: return@mapIndexedNotNull null
            val value = element.text.trim()
            if (value.isEmpty()) return@mapIndexedNotNull null
            Word(
                text = value,
                index = index,
                startIndex = 0,
                endIndex = value.length,
                bounds = RectF(bounds)
            )
        }.ifEmpty {
            val value = text.trim()
            if (value.isEmpty()) return null
            listOf(
                Word(
                    text = value,
                    index = 0,
                    startIndex = 0,
                    endIndex = value.length,
                    bounds = RectF(lineBounds)
                )
            )
        }

        return TextNode(
            id = UUID.randomUUID().toString(),
            fullText = text,
            bounds = Rect(lineBounds),
            words = words
        )
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
