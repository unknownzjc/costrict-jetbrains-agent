// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

interface FileTypeHandler {
    fun canHandle(fileName: String): Boolean
    fun shouldProcessElement(element: PsiElement): Boolean
    fun createLineMarker(element: PsiElement, document: Document): LineMarkerInfo<*>?
}