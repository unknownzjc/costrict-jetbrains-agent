// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import javax.swing.Icon

object LineMarkerFactory {
    fun create(
        element: PsiElement,
        icon: Icon,
        tooltipText: String,
        action: AnAction
    ): LineMarkerInfo<*> {
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltipText },
            { _, _ ->
                val event = AnActionEvent.createFromDataContext(
                    "WorkflowLineMarker",
                    null,
                    DataContext { dataId ->
                        when (dataId) {
                            CommonDataKeys.PROJECT.name -> element.project
                            CommonDataKeys.PSI_ELEMENT.name -> element
                            else -> null
                        }
                    }
                )
                action.actionPerformed(event)
            },
            GutterIconRenderer.Alignment.CENTER,
            { tooltipText }
        )
    }
}