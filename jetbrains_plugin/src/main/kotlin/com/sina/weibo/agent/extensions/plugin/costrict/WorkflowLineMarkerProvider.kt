// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vfs.VirtualFile
import com.sina.weibo.agent.actions.WorkflowMenuAction
import com.sina.weibo.agent.actions.SyncToDesignAction
import com.sina.weibo.agent.actions.SyncToTasksAction

/**
 * Costrict extension provider implementation
 * 为 .cospec 目录下的 requirements.md、design.md 和 tasks.md 文件添加行标记按钮
 */
class WorkflowLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val text = element.text ?: return null
        
        // 获取文件路径，判断是否为 .cospec 目录下的文件
        val containingFile = element.containingFile
        val virtualFile = containingFile?.virtualFile ?: return null
        val filePath = virtualFile.path
        
        // 检查是否在 .cospec 目录下
        if (!filePath.contains("/.cospec/")) {
            return null
        }
        
        // 获取文件名
        val fileName = virtualFile.name
        
        // 确保只处理行首的元素，避免一个行创建多个标记
        // 检查当前元素是否是行的第一个元素
        if (!isFirstElementInLine(element)) {
            return null
        }
        
        // 根据不同文件类型处理
        when (fileName) {
            "requirements.md" -> {
                // requirements.md 需求文档，为所有标题（包括一级、二级、三级等）添加"同步变更至设计"按钮
                // 确保只处理实际的标题行，而不是标题中的每个元素
                // 检查元素是否是行首元素
                val elementText = element.text
                val lineStart = elementText.startsWith("#") || (elementText.trimStart().startsWith("#") && elementText.indexOf('#') < 5)
                
                if (lineStart) {
                    val syncToDesign = SyncToDesignAction("同步变更至设计")
                    
                    return Info(
                        AllIcons.Actions.CheckOut,
                        { "同步变更至设计" },
                        syncToDesign
                    )
                }

                return null // 如果不是标题行，则不添加标记
            }
            
            "design.md" -> {
                // design.md 设计文档，为所有标题（包括一级、二级、三级等）添加"同步变更至任务"按钮
                // 确保只处理实际的标题行，而不是标题中的每个元素
                // 检查元素是否是行首元素
                val elementText = element.text
                val lineStart = elementText.startsWith("#") || (elementText.trimStart().startsWith("#") && elementText.indexOf('#') < 5)
                
                if (lineStart) {
                    val syncToTasks = SyncToTasksAction("同步变更至任务")
                    
                    return Info(
                        AllIcons.Actions.CheckOut,
                        { "同步变更至任务" },
                        syncToTasks
                    )
                }

                return null // 如果不是任务项，则不添加标记
            }
            
            "tasks.md" -> {
                // 确保文本不为空且包含内容
                if (text.isNullOrBlank()) {
                    return null
                }
                
                val trimmedText = text.trimStart()
                
                // tasks.md 任务列表文档，为每个任务项根据状态添加统一操作按钮
                // 检测任务项：以 "- [ ]"、"- [x]" 或 "- [-]" 开头的行
                if (trimmedText.startsWith("- [") &&
                    (trimmedText.startsWith("- [ ]") || // 待执行的任务
                     trimmedText.startsWith("- [x]") || // 已完成的任务
                     trimmedText.startsWith("- [-]"))) { // 进行中的任务
                    
                    // 检查是否是第一个任务块
                    val isFirstTask = checkIfFirstTask(element)
                    
                    // 根据任务状态创建统一的菜单操作
                    val taskStatus = when {
                        trimmedText.startsWith("- [ ]") -> WorkflowMenuAction.TaskStatus.PENDING
                        trimmedText.startsWith("- [-]") -> WorkflowMenuAction.TaskStatus.IN_PROGRESS
                        trimmedText.startsWith("- [x]") -> WorkflowMenuAction.TaskStatus.COMPLETED
                        else -> null
                    }
                    
                    taskStatus?.let { status ->
                        val menuAction = WorkflowMenuAction(status, trimmedText, isFirstTask)
                        val statusText = when (status) {
                            WorkflowMenuAction.TaskStatus.PENDING -> "待执行任务"
                            WorkflowMenuAction.TaskStatus.IN_PROGRESS -> "进行中任务"
                            WorkflowMenuAction.TaskStatus.COMPLETED -> "已完成任务"
                        }
                        
                        return Info(
                            AllIcons.RunConfigurations.TestState.Run,
                            { statusText },
                            menuAction
                        )
                    }
                }

                return null // 如果不是任务项，则不添加标记
            }
        }

        return null
    }
    
    /**
     * 检查元素是否是行的第一个元素
     */
    private fun isFirstElementInLine(element: PsiElement): Boolean {
        // 首先检查元素文本是否为空或仅包含空白字符
        if (element.text.isNullOrBlank()) {
            return false
        }
        
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) ?: return false
        val lineStartOffset = document.getLineStartOffset(document.getLineNumber(element.textOffset))
        val lineEndOffset = document.getLineEndOffset(document.getLineNumber(element.textOffset))
        
        // 获取完整的行文本
        val fullLineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset))
        val trimmedLineText = fullLineText.trimStart()
        
        // 检查元素是否匹配行首的任务模式
        val elementText = element.text.trim()
        val isTaskLine = trimmedLineText.startsWith("- [") &&
            (trimmedLineText.startsWith("- [ ]") ||
             trimmedLineText.startsWith("- [x]") ||
             trimmedLineText.startsWith("- [-]"))
        
        // 如果是任务行，检查元素是否包含任务的起始部分
        if (isTaskLine) {
            // 检查元素文本是否包含任务标记的开头部分
            if (elementText.contains("- [") || elementText.contains("-") || elementText.contains("[")) {
                // 进一步检查元素是否位于行首的非空白位置
                val elementOffsetInLine = element.textOffset - lineStartOffset
                val leadingWhitespace = fullLineText.take(elementOffsetInLine)
                
                // 只有当元素前面只有空白字符时才返回true
                return leadingWhitespace.isBlank()
            }
        }
        
        // 对于标题行的检查
        if (elementText.startsWith("#") || elementText.trimStart().startsWith("#")) {
            val elementOffsetInLine = element.textOffset - lineStartOffset
            val leadingWhitespace = fullLineText.take(elementOffsetInLine)
            return leadingWhitespace.isBlank()
        }
        
        return false
    }
    
    /**
     * 检查当前任务是否是文档中的第一个任务块
     */
    private fun checkIfFirstTask(element: PsiElement): Boolean {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) ?: return false
        val currentLineNumber = document.getLineNumber(element.textOffset)
        
        // 从文档开始遍历，查找第一个任务块
        for (lineNum in 0 until document.lineCount) {
            val lineText = document.getText(com.intellij.openapi.util.TextRange(
                document.getLineStartOffset(lineNum),
                document.getLineEndOffset(lineNum)
            ))
            
            // 检查是否是任务行
            if (lineText.trimStart().startsWith("- [") &&
                (lineText.trimStart().startsWith("- [ ]") ||
                 lineText.trimStart().startsWith("- [x]") ||
                 lineText.trimStart().startsWith("- [-]"))) {
                
                // 如果当前行就是我们检查的行，那么它就是第一个任务
                return lineNum == currentLineNumber
            }
        }
        
        return false
    }
}