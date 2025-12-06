// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.sina.weibo.agent.actions.SyncToDesignAction

class RequirementsFileHandler : FileTypeHandler {
    override fun canHandle(fileName: String): Boolean {
        return fileName == CostrictFileConstants.REQUIREMENTS_FILE
    }
    
    override fun shouldProcessElement(element: PsiElement): Boolean {
        return isHeaderElement(element) && PsiElementUtils.isFirstElementInLine(element)
    }
    
    override fun createLineMarker(element: PsiElement, document: Document): LineMarkerInfo<*>? {
        val action = SyncToDesignAction(CostrictFileConstants.TOOLTIP_SYNC_TO_DESIGN)
        return LineMarkerFactory.create(
            element,
            AllIcons.Actions.CheckOut,
            CostrictFileConstants.TOOLTIP_SYNC_TO_DESIGN,
            action
        )
    }
    
    private fun isHeaderElement(element: PsiElement): Boolean {
        val elementText = element.text
        return elementText.startsWith("#") || 
               (elementText.trimStart().startsWith("#") && elementText.indexOf('#') < 5)
    }
}