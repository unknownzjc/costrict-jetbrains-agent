// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.regex.Pattern

object TaskBlockParser {
    private val TASK_PATTERN = Pattern.compile("^\\s*-\\s*\\[([ x-])\\]\\s+")
    
    data class TaskBlock(val startLine: Int, val endLine: Int, val status: String?)
    
    fun parseTaskBlocks(document: Document): List<TaskBlock> {
        val taskBlocks = mutableListOf<TaskBlock>()
        var currentTaskStartLine = -1
        var currentTaskStatus: String? = null
        
        for (line in 0 until document.lineCount) {
            val lineText = getLineText(document, line)
            val matcher = TASK_PATTERN.matcher(lineText.trim())
            
            if (matcher.find()) {
                if (currentTaskStartLine != -1) {
                    taskBlocks.add(TaskBlock(currentTaskStartLine, line - 1, currentTaskStatus))
                }
                currentTaskStartLine = line
                currentTaskStatus = matcher.group(1).lowercase()
            } else if (currentTaskStartLine != -1) {
                val isContinuation = isTaskBlockContinuation(lineText)
                val isEmptyLine = lineText.trim().isEmpty()
                
                if (!isContinuation || isEmptyLine) {
                    taskBlocks.add(TaskBlock(currentTaskStartLine, line - 1, currentTaskStatus))
                    currentTaskStartLine = -1
                    currentTaskStatus = null
                }
            }
        }
        
        if (currentTaskStartLine != -1) {
            taskBlocks.add(TaskBlock(currentTaskStartLine, document.lineCount - 1, currentTaskStatus))
        }
        
        return taskBlocks
    }
    
    fun isTaskBlockContinuation(lineText: String): Boolean {
        val trimmedText = lineText.trim()
        if (trimmedText.isEmpty()) return false
        
        val taskMatcher = TASK_PATTERN.matcher(trimmedText)
        if (taskMatcher.find()) return false
        
        if (trimmedText.startsWith("#")) return false
        
        return lineText.startsWith(" ") || lineText.startsWith("\t")
    }
    
    fun isFirstTaskBlock(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile ?: return false
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return false
        val currentLineNumber = document.getLineNumber(element.textOffset)
        
        val taskBlocks = parseTaskBlocks(document)
        return taskBlocks.firstOrNull()?.let { block ->
            currentLineNumber in block.startLine..block.endLine
        } ?: false
    }
    
    private fun getLineText(document: Document, line: Int): String {
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        return document.getText(TextRange(startOffset, endOffset))
    }
}