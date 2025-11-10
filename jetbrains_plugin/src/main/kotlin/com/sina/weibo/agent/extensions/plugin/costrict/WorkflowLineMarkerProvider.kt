// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vfs.VirtualFile
import com.sina.weibo.agent.actions.WorkflowRunCommandAction
import com.sina.weibo.agent.actions.WorkflowDebugRunCommandAction
import com.sina.weibo.agent.actions.WorkflowEditConfigAction
import com.sina.weibo.agent.actions.SyncToDesignAction
import com.sina.weibo.agent.actions.SyncToTasksAction
import com.sina.weibo.agent.actions.RunTaskAction
import com.sina.weibo.agent.actions.RunAllTasksAction
import com.sina.weibo.agent.actions.GenerateTestsAction

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
            }
            
            "tasks.md" -> {
                // tasks.md 任务列表文档，为每个任务项根据状态添加不同按钮组合
                // 检测任务项：以 "- [ ]"、"- [x]" 或 "- [-]" 开头的行
                if (text.trimStart().startsWith("- [") &&
                    (text.trimStart().startsWith("- [ ]") || // 待执行的任务
                     text.trimStart().startsWith("- [x]") || // 已完成的任务
                     text.trimStart().startsWith("- [-]"))) { // 进行中的任务
                    
                    val trimmedText = text.trimStart()
                    
                    // 根据任务状态选择不同的图标和提示文本
                    when {
                        trimmedText.startsWith("- [ ]") -> {
                            // 待执行任务：运行、运行所有任务、生成测试用例
                            val runAction = RunTaskAction("运行")
                            val runAllAction = RunAllTasksAction("运行所有任务")
                            val generateTestsAction = GenerateTestsAction("生成测试用例")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "待执行任务" },
                                runAction, runAllAction, generateTestsAction
                            )
                        }
                        trimmedText.startsWith("- [-]") -> {
                            // 进行中任务：使用动画图标表示正在运行，点击时显示重试按钮
                            val retryAction = WorkflowDebugRunCommandAction("重试")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "进行中任务" },
                                retryAction
                            )
                        }
                        trimmedText.startsWith("- [x]") -> {
                            // 已完成任务：重试、运行所有任务、生成测试用例
                            val retryAction = WorkflowDebugRunCommandAction("重试")
                            val runAllAction = RunAllTasksAction("运行所有任务")
                            val generateTestsAction = GenerateTestsAction("生成测试用例")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "已完成任务" },
                                retryAction, runAllAction, generateTestsAction
                            )
                        }
                    }
                }
            }
        }

        return null
    }
    
    /**
     * 检查元素是否是行的第一个元素
     */
    private fun isFirstElementInLine(element: PsiElement): Boolean {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) ?: return false
        val lineStartOffset = document.getLineStartOffset(document.getLineNumber(element.textOffset))
        return element.textOffset == lineStartOffset || element.text.trim().startsWith("#")
    }
}