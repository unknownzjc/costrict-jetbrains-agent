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
import com.sina.weibo.agent.actions.WorkflowMenuAction

class TasksFileHandler : FileTypeHandler {
    override fun canHandle(fileName: String): Boolean {
        return fileName == CostrictFileConstants.TASKS_FILE
    }
    
    override fun shouldProcessElement(element: PsiElement): Boolean {
        val elementText = element.text.trimStart()
        val isTaskElement = elementText.startsWith(CostrictFileConstants.TASK_PENDING) ||
                           elementText.startsWith(CostrictFileConstants.TASK_IN_PROGRESS) ||
                           elementText.startsWith(CostrictFileConstants.TASK_COMPLETED)
        
        if (!isTaskElement) return false
        
        // 确保这是行的第一个元素，避免子项被重复处理
        return PsiElementUtils.isFirstElementInLine(element)
    }
    
    override fun createLineMarker(element: PsiElement, document: Document): LineMarkerInfo<*>? {
        val trimmedText = element.text.trimStart()
        val status = determineTaskStatus(trimmedText) ?: return null
        
        // 获取当前行号（0-based）
        val lineNumber = document.getLineNumber(element.textOffset)
        
        // 检查是否为第一个任务
        val isFirstTask = isFirstTask(element, lineNumber)
        
        // 根据是否为第一个任务创建不同的Action
        val action = if (isFirstTask) {
            // 第一个任务使用下拉菜单，包含运行、运行所有、生成测试用例
            com.sina.weibo.agent.actions.WorkflowMenuAction(
                status,
                trimmedText,
                isFirstTask = true
            )
        } else {
            // 其他任务使用单独的RunTaskAction
            com.sina.weibo.agent.actions.RunTaskAction(lineNumber)
        }
        
        val statusText = getStatusText(status)
        return LineMarkerFactory.create(
            element,
            AllIcons.RunConfigurations.TestState.Run,
            statusText,
            action
        )
    }
    
    private fun determineTaskStatus(text: String): WorkflowMenuAction.TaskStatus? {
        return when {
            text.startsWith(CostrictFileConstants.TASK_PENDING) -> 
                WorkflowMenuAction.TaskStatus.PENDING
            text.startsWith(CostrictFileConstants.TASK_IN_PROGRESS) -> 
                WorkflowMenuAction.TaskStatus.IN_PROGRESS
            text.startsWith(CostrictFileConstants.TASK_COMPLETED) -> 
                WorkflowMenuAction.TaskStatus.COMPLETED
            else -> null
        }
    }
    
    private fun getStatusText(status: WorkflowMenuAction.TaskStatus): String {
        return when (status) {
            WorkflowMenuAction.TaskStatus.PENDING ->
                CostrictFileConstants.STATUS_PENDING_TEXT
            WorkflowMenuAction.TaskStatus.IN_PROGRESS ->
                CostrictFileConstants.STATUS_IN_PROGRESS_TEXT
            WorkflowMenuAction.TaskStatus.COMPLETED ->
                CostrictFileConstants.STATUS_COMPLETED_TEXT
        }
    }
    
    /**
     * 检查是否为第一个任务
     */
    private fun isFirstTask(element: PsiElement, currentLineNumber: Int): Boolean {
        // 获取文件内容
        val fileContent = element.containingFile?.text ?: return false
        
        // 按行分割
        val lines = fileContent.split("\n")
        
        // 遍历所有行，查找第一个任务项
        for ((index, line) in lines.withIndex()) {
            val trimmedLine = line.trimStart()
            // 检查是否为任务项（- [ ], - [-], - [x]）
            if (trimmedLine.startsWith(CostrictFileConstants.TASK_PENDING) ||
                trimmedLine.startsWith(CostrictFileConstants.TASK_IN_PROGRESS) ||
                trimmedLine.startsWith(CostrictFileConstants.TASK_COMPLETED)) {
                // 找到第一个任务，检查是否匹配当前行
                return index == currentLineNumber
            }
        }
        
        return false
    }
}