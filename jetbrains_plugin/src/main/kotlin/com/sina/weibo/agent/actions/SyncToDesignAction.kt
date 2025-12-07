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
 * 同步需求到设计的操作类
 * 继承 WorkflowActionBase，实现同步需求到设计的逻辑
 */
class SyncToDesignAction : WorkflowActionBase(
    actionName = "同步需求到设计",
    rpcCommand = "zgsm.workflow.syncToDesignJetbrains",
    actionType = "sync_to_design"
) {
    
    override fun collectParams(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): WorkflowActionParams {
        // 验证文件类型
        if (virtualFile.name != CostrictFileConstants.REQUIREMENTS_FILE) {
            throw IllegalStateException("此操作只能在 requirements.md 文件中执行")
        }
        
        val document = editor.document
        val caretModel = editor.caretModel
        val lineNumber = document.getLineNumber(caretModel.offset)
        
        // 获取整个 requirements.md 内容
        val requirementsContent = getFileContent(editor)
        
        return WorkflowActionParams(
            filePath = virtualFile.path,
            documentType = "requirements",
            lineNumber = lineNumber,
            requirementsContent = requirementsContent,
            actionType = "sync_to_design"
        )
    }
}