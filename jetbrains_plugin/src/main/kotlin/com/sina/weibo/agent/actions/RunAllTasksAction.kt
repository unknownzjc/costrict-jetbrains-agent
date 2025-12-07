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
 * 运行所有任务的操作类
 * 继承 WorkflowActionBase，实现运行所有任务的逻辑
 */
class RunAllTasksAction : WorkflowActionBase(
    actionName = "运行所有任务",
    rpcCommand = "zgsm.workflow.runAllTasksJetbrains",
    actionType = "run_all"
) {
    
    override fun collectParams(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): WorkflowActionParams {
        // 验证文件类型
        if (virtualFile.name != CostrictFileConstants.TASKS_FILE) {
            throw IllegalStateException("此操作只能在 tasks.md 文件中执行")
        }
        
        val document = editor.document
        val caretModel = editor.caretModel
        val lineNumber = document.getLineNumber(caretModel.offset)
        
        // 获取整个 tasks.md 内容
        val allTasksContent = getFileContent(editor)
        
        return WorkflowActionParams(
            filePath = virtualFile.path,
            documentType = "tasks",
            lineNumber = lineNumber,
            allTasksContent = allTasksContent,
            actionType = "run_all"
        )
    }
}