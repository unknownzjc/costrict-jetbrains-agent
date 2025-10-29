// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.comments

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color

// Comment Controller data class
data class CommentController(
    val handle: Int,
    val id: String,
    val label: String,
    val extensionId: String,
    val features: CommentControllerFeatures = CommentControllerFeatures()
)

data class CommentControllerFeatures(
    val options: Map<String, Any?>? = null,
    val reactionHandler: Boolean = false,
    val resourceHints: Map<String, Any?>? = null,
)

// Comment Thread data class
data class CommentThread(
    val handle: Int,
    val threadId: String,
    val controllerHandle: Int,
    var resource: Map<String, Any?>,
    var range: CommentRange,
    val extensionId: String,
    var comments: List<Map<String, Any?>>,
    var collapseState: String? = null,
    var canReply: Boolean = true,
    var isTemplate: Boolean = false,
    var editorHandleId: String? = null,
    var lineHighlighter: RangeHighlighter? = null,
    var rangeHighlighter: RangeHighlighter? = null,
    var isVisible: Boolean = true,
    var manuallyCollapsed: Boolean = false
) {
    fun toSerializableMap(): Map<String, Any?> = mapOf(
        "handle" to handle,
        "threadId" to threadId,
        "controllerHandle" to controllerHandle,
        "resource" to resource,
        "range" to range.toMap(),
        "comments" to comments,
        "collapseState" to (collapseState ?: if (isVisible) "expanded" else "collapsed"),
        "canReply" to canReply,
        "isTemplate" to isTemplate,
        "extensionId" to extensionId
    )
}

data class CommentRange(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
) {
    companion object {
        fun fromMap(range: Map<String, Any?>?): CommentRange {
            if (range == null) {
                return CommentRange(1, 1, 1, 1)
            }
            val startLine = (range["startLineNumber"] as? Number)?.toInt() ?: 1
            val startColumn = (range["startColumn"] as? Number)?.toInt() ?: 1
            val endLine = (range["endLineNumber"] as? Number)?.toInt() ?: startLine
            val endColumn = (range["endColumn"] as? Number)?.toInt() ?: startColumn
            return CommentRange(startLine, startColumn, endLine, endColumn)
        }
    }

    fun toZeroBasedLines(): Pair<Int, Int> = Pair(
        (startLine - 1).coerceAtLeast(0),
        (endLine - 1).coerceAtLeast(0)
    )

    fun startColumnZeroBased(): Int = (startColumn - 1).coerceAtLeast(0)

    fun endColumnZeroBased(document: Document, endLineZeroBased: Int): Int {
        val zeroBased = (endColumn - 1).coerceAtLeast(0)
        if (endLineZeroBased !in 0 until document.lineCount) {
            return zeroBased
        }
        val lineStart = document.getLineStartOffset(endLineZeroBased)
        val lineEnd = document.getLineEndOffset(endLineZeroBased)
        val lineLength = (lineEnd - lineStart).coerceAtLeast(0)
        return zeroBased.coerceAtMost(lineLength)
    }

    fun startOffset(document: Document): Int {
        val (startLineZero, _) = toZeroBasedLines()
        if (startLineZero !in 0 until document.lineCount) {
            return 0
        }
        val lineStart = document.getLineStartOffset(startLineZero)
        val lineEnd = document.getLineEndOffset(startLineZero)
        val column = startColumnZeroBased().coerceAtMost(lineEnd - lineStart)
        return lineStart + column
    }

    fun endOffset(document: Document): Int {
        val (_, endLineZero) = toZeroBasedLines()
        if (endLineZero !in 0 until document.lineCount) {
            return document.textLength
        }
        val lineStart = document.getLineStartOffset(endLineZero)
        val lineEnd = document.getLineEndOffset(endLineZero)
        val column = endColumnZeroBased(document, endLineZero).coerceAtMost(lineEnd - lineStart)
        return lineStart + column
    }

    fun toMap(): Map<String, Int> = mapOf(
        "startLineNumber" to startLine,
        "startColumn" to startColumn,
        "endLineNumber" to endLine,
        "endColumn" to endColumn
    )
}

// Gutter icon renderer
class CommentGutterIconRenderer(
    private val commentManager: CommentManager,
    private val thread: CommentThread
) : GutterIconRenderer() {
    override fun getIcon() = AllIcons.General.Balloon

    override fun getTooltipText(): String {
        return "查看评论 (${thread.comments.size}) — 再次点击可关闭"
    }

    override fun getClickAction(): AnAction = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            commentManager.showThreadPopup(thread.handle)
        }
    }

    override fun isNavigateAction(): Boolean = true

    override fun equals(other: Any?): Boolean =
        other is CommentGutterIconRenderer && other.thread.handle == thread.handle

    override fun hashCode(): Int = thread.handle
}

val COMMENT_TEXT_ATTRIBUTES: TextAttributes = TextAttributes().apply {
    backgroundColor = JBColor(
        Color(0xE6, 0xF0, 0xFF, 0x80),
        Color(0x28, 0x3A, 0x4D, 0x80)
    )
    effectColor = JBColor(
        Color(0x66, 0x99, 0xCC),
        Color(0x1F, 0x5B, 0x7F)
    )
    effectType = EffectType.ROUNDED_BOX
}
