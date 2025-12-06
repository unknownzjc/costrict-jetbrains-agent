// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

object PsiElementUtils {
    /**
     * 检查元素是否是行的第一个元素
     * 用于防止同一行内的多个 PSI 元素被重复处理
     */
    fun isFirstElementInLine(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile ?: return false
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return false
        
        val lineNumber = document.getLineNumber(element.textOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        
        // 计算元素在行内的偏移
        val elementOffsetInLine = element.textOffset - lineStartOffset
        
        // 如果元素在行首（偏移为0）或元素前只有空白字符
        return elementOffsetInLine == 0 || isOnlyWhitespaceBefore(document, lineStartOffset, element.textOffset)
    }
    
    /**
     * 检查元素前是否只有空白字符
     */
    private fun isOnlyWhitespaceBefore(document: Document, lineStartOffset: Int, elementOffset: Int): Boolean {
        if (elementOffset <= lineStartOffset) return true
        
        val textBeforeElement = document.getText(TextRange(lineStartOffset, elementOffset))
        return textBeforeElement.isBlank()
    }
    
    /**
     * 获取元素所在的行号
     */
    fun getLineNumber(element: PsiElement): Int? {
        val file = element.containingFile ?: return null
        val virtualFile = file.virtualFile ?: return null
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null
        
        return document.getLineNumber(element.textOffset)
    }
}