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
 * 同步设计到任务的操作类
 * 继承 WorkflowActionBase，实现同步设计到任务的逻辑
 */
class SyncToTasksAction : WorkflowActionBase(
    actionName = "同步设计到任务",
    rpcCommand = "zgsm.coworkflow.syncToTasksJetbrains",
    actionType = "sync_to_tasks"
) {
    
    override fun collectParams(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): WorkflowActionParams {
        // 验证文件类型
        if (virtualFile.name != CostrictFileConstants.DESIGN_FILE) {
            throw IllegalStateException("此操作只能在 design.md 文件中执行")
        }
        
        val document = editor.document
        val caretModel = editor.caretModel
        val lineNumber = document.getLineNumber(caretModel.offset)
        
        // 获取整个 design.md 内容
        val designContent = getFileContent(editor)
        
        return WorkflowActionParams(
            filePath = virtualFile.path,
            documentType = "design",
            lineNumber = lineNumber,
            designContent = designContent,
            actionType = "sync_to_tasks"
        )
    }
}