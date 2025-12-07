// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.sina.weibo.agent.extensions.plugin.costrict.CostrictFileConstants

/**
 * 运行单个任务的操作类
 * 继承 WorkflowActionBase，实现运行单个任务的逻辑
 */
class RunTaskAction : WorkflowActionBase(
    actionName = "运行任务",
    rpcCommand = "zgsm.workflow.runTaskJetbrains",
    actionType = "run"
) {
    
    override fun collectParams(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): WorkflowActionParams {
        val document = editor.document
        val caretModel = editor.caretModel
        val lineNumber = document.getLineNumber(caretModel.offset)
        
        // 获取当前行的任务文本
        val taskText = getTaskText(editor, psiFile, lineNumber)
        if (taskText.isNullOrBlank()) {
            logger.warn("RunTaskAction: 未找到任务文本")
            throw IllegalStateException("未找到任务文本，请确保光标位于任务行上")
        }
        
        // 获取任务状态
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset))
        val taskStatus = getTaskStatus(lineText)
        
        // 检查是否为第一个任务
        val isFirstTask = isFirstTask(editor, psiFile, lineNumber)
        // val isFirstTask = isFirstTask(psiFile)
        
        return WorkflowActionParams(
            filePath = virtualFile.path,
            documentType = "tasks",
            lineNumber = lineNumber,
            taskText = taskText,
            taskStatus = taskStatus,
            isFirstTask = isFirstTask,
            actionType = "run"
        )
    }
}