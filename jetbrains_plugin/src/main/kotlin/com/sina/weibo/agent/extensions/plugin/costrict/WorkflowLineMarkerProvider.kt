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
        
        // 根据不同文件类型处理
        when (fileName) {
            "requirements.md" -> {
                // requirements.md 需求文档，每个一级标题都加上按钮
                // 检测一级标题：以 "# " 开头但不是以 "## " 开头的行
                if (text.trimStart().startsWith("# ") && !text.trimStart().startsWith("## ")) {
                    val titleText = text.trimStart().substring(1).trim()
                    val runAction = WorkflowRunCommandAction("执行需求: $titleText")
                    val debugAction = WorkflowDebugRunCommandAction("调试需求: $titleText")
                    val editAction = WorkflowEditConfigAction("编辑需求配置: $titleText")
                    
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        { "执行需求: $titleText" },
                        runAction, debugAction, editAction
                    )
                }
            }
            
            "design.md" -> {
                // design.md 设计文档，每个一级标题都加上按钮
                if (text.trimStart().startsWith("# ") && !text.trimStart().startsWith("## ")) {
                    val titleText = text.trimStart().substring(1).trim()
                    val runAction = WorkflowRunCommandAction("执行设计: $titleText")
                    val debugAction = WorkflowDebugRunCommandAction("调试设计: $titleText")
                    val editAction = WorkflowEditConfigAction("编辑设计配置: $titleText")
                    
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        { "执行设计: $titleText" },
                        runAction, debugAction, editAction
                    )
                }
            }
            
            "tasks.md" -> {
                // tasks.md 任务列表文档，为每个任务项添加按钮
                // 检测任务项：以 "- [ ]"、"- [x]" 或 "- [-]" 开头的行
                if (text.trimStart().startsWith("- [") &&
                    (text.trimStart().startsWith("- [ ]") ||
                     text.trimStart().startsWith("- [x]") ||
                     text.trimStart().startsWith("- [-]"))) {
                    
                    val taskText = text.trimStart().substringAfter("] ")
                    val trimmedText = text.trimStart()
                    
                    // 根据任务状态选择不同的图标和提示文本
                    when {
                        trimmedText.startsWith("- [-]") -> {
                            // 进行中的任务使用 loading 图标
                            val runAction = WorkflowRunCommandAction("执行任务: $taskText")
                            val debugAction = WorkflowDebugRunCommandAction("调试任务: $taskText")
                            val editAction = WorkflowEditConfigAction("编辑任务配置: $taskText")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "执行中: $taskText" },
                                runAction, debugAction, editAction
                            )
                        }
                        trimmedText.startsWith("- [ ]") -> {
                            // 待办任务使用默认图标
                            val runAction = WorkflowRunCommandAction("执行任务: $taskText")
                            val debugAction = WorkflowDebugRunCommandAction("调试任务: $taskText")
                            val editAction = WorkflowEditConfigAction("编辑任务配置: $taskText")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "执行任务(待办): $taskText" },
                                runAction, debugAction, editAction
                            )
                        }
                        trimmedText.startsWith("- [x]") -> {
                            // 已完成任务使用默认图标
                            val runAction = WorkflowRunCommandAction("执行任务: $taskText")
                            val debugAction = WorkflowDebugRunCommandAction("调试任务: $taskText")
                            val editAction = WorkflowEditConfigAction("编辑任务配置: $taskText")
                            
                            return Info(
                                AllIcons.RunConfigurations.TestState.Run,
                                { "执行任务(已完成): $taskText" },
                                runAction, debugAction, editAction
                            )
                        }
                    }
                }
            }
        }

        return null
    }
}