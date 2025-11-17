// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * 工作流任务菜单操作类
 * 提供一个统一的入口，点击后弹出包含多个操作的子菜单
 */
class WorkflowMenuAction(
    private val taskStatus: TaskStatus,
    private val taskText: String = "",
    private val isFirstTask: Boolean = false
) : AnAction() {
    
    enum class TaskStatus {
        PENDING,    // 待执行
        IN_PROGRESS,// 进行中
        COMPLETED   // 已完成
    }

    override fun actionPerformed(e: AnActionEvent) {
        println("WorkflowMenuAction: 开始执行 actionPerformed")
        println("WorkflowMenuAction: 任务状态: $taskStatus, 任务文本: $taskText, 是否第一个任务: $isFirstTask")
        
        // 获取编辑器组件作为弹出菜单的owner
        val project = e.project ?: run {
            println("WorkflowMenuAction: 项目为空，返回")
            return
        }
        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
        val owner = editor?.component
        
        println("WorkflowMenuAction: 获取到的编辑器: ${editor?.javaClass?.simpleName}, owner组件: ${owner?.javaClass?.simpleName}")
        
        // 创建一个弹出菜单
        val popup = JPopupMenu()
        
        // 根据任务状态添加不同的操作
        when (taskStatus) {
            TaskStatus.PENDING -> {
                val runAction = JMenuItem("运行")
                runAction.addActionListener {
                    try {
                        println("WorkflowMenuAction: 运行任务按钮被点击")
                        // 创建新的事件对象来传递正确的上下文
                        val newAction = RunTaskAction("运行")
                        // 使用当前项目和上下文创建新的事件
                        newAction.actionPerformed(e)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("WorkflowMenuAction: 运行任务失败 - ${ex.message}")
                    }
                }
                popup.add(runAction)
                
                // 只有第一个任务才显示"运行所有任务"和"生成测试用例"
                if (isFirstTask) {
                    val runAllAction = JMenuItem("运行所有任务")
                    runAllAction.addActionListener {
                        try {
                            RunAllTasksAction("运行所有任务").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    val generateTestsAction = JMenuItem("生成测试用例")
                    generateTestsAction.addActionListener {
                        try {
                            GenerateTestsAction("生成测试用例").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                        }
                    }
                    popup.add(generateTestsAction)
                }
            }
            TaskStatus.IN_PROGRESS -> {
                // 运行中状态显示为状态文本，不是可点击的操作
                val inProgressStatus = JMenuItem("运行中")
                inProgressStatus.isEnabled = false  // 禁用点击，仅作为状态显示
                popup.add(inProgressStatus)
                
                // 只有第一个任务才显示"运行所有任务"和"生成测试用例"
                if (isFirstTask) {
                    val runAllAction = JMenuItem("运行所有任务")
                    runAllAction.addActionListener {
                        try {
                            RunAllTasksAction("运行所有任务").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    val generateTestsAction = JMenuItem("生成测试用例")
                    generateTestsAction.addActionListener {
                        try {
                            GenerateTestsAction("生成测试用例").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                        }
                    }
                    popup.add(generateTestsAction)
                } else {
                     val retryAction = JMenuItem("重试")
                    retryAction.addActionListener {
                        try {
                            println("WorkflowMenuAction: 重试按钮被点击")
                            WorkflowDebugRunCommandAction("重试").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 重试失败 - ${ex.message}")
                        }
                    }
                    popup.add(retryAction)
                }
            }
            TaskStatus.COMPLETED -> {
                val retryAction = JMenuItem("重试")
                retryAction.addActionListener {
                    try {
                        println("WorkflowMenuAction: 重试按钮被点击")
                        WorkflowDebugRunCommandAction("重试").actionPerformed(e)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        println("WorkflowMenuAction: 重试失败 - ${ex.message}")
                    }
                }
                popup.add(retryAction)
                
                // 只有第一个任务才显示"运行所有任务"和"生成测试用例"
                if (isFirstTask) {
                    val runAllAction = JMenuItem("运行所有任务")
                    runAllAction.addActionListener {
                        try {
                            println("WorkflowMenuAction: 运行所有任务按钮被点击")
                            RunAllTasksAction("运行所有任务").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    val generateTestsAction = JMenuItem("生成测试用例")
                    generateTestsAction.addActionListener {
                        try {
                            println("WorkflowMenuAction: 生成测试用例按钮被点击")
                            GenerateTestsAction("生成测试用例").actionPerformed(e)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                        }
                    }
                    popup.add(generateTestsAction)
                }
            }
        }
        
        // 获取输入事件的位置信息
        val inputEvent = e.inputEvent
        println("WorkflowMenuAction: 输入事件类型: ${inputEvent?.javaClass?.simpleName}")
        
        if (inputEvent is java.awt.event.MouseEvent && owner != null) {
            println("WorkflowMenuAction: 处理鼠标事件，坐标: (${inputEvent.x}, ${inputEvent.y})")
            // 将输入事件的位置转换为owner组件的坐标
            val point = java.awt.Point(inputEvent.x, inputEvent.y)
            // 转换为相对于owner组件的坐标
            val source = inputEvent.source
            if (source is java.awt.Component) {
                println("WorkflowMenuAction: 事件源组件: ${source.javaClass.simpleName}")
                println("WorkflowMenuAction: owner组件: ${owner.javaClass.simpleName}")
                println("WorkflowMenuAction: source.isShowing: ${source.isShowing}, owner.isShowing: ${owner.isShowing}")
                
                // 检查组件是否已显示在屏幕上
                if (source.isShowing && owner.isShowing) {
                    try {
                        // 将事件源组件的坐标转换为owner组件的坐标
                        val sourceLocation = source.locationOnScreen
                        val ownerLocation = owner.locationOnScreen
                        val relativeX = sourceLocation.x - ownerLocation.x + inputEvent.x
                        val relativeY = sourceLocation.y - ownerLocation.y + inputEvent.y
                        println("WorkflowMenuAction: 在相对位置显示弹出菜单: ($relativeX, $relativeY)")
                        popup.show(owner, relativeX, relativeY)
                        return
                    } catch (e: java.awt.IllegalComponentStateException) {
                        println("WorkflowMenuAction: 坐标转换失败: ${e.message}")
                        // 如果获取位置失败，继续使用回退方案
                    }
                }
                // 回退方案：直接使用鼠标事件坐标
                println("WorkflowMenuAction: 使用回退方案1，在 (${point.x}, ${point.y}) 显示弹出菜单")
                popup.show(owner, point.x, point.y)
            } else {
                println("WorkflowMenuAction: 事件源不是组件，使用回退方案2")
                popup.show(owner, point.x, point.y)
            }
        } else {
            println("WorkflowMenuAction: 没有鼠标事件或owner为空，使用鼠标当前位置")
            // 回退方案：使用鼠标当前位置
            val mouseLocation = java.awt.MouseInfo.getPointerInfo()?.location
            if (mouseLocation != null && owner != null) {
                println("WorkflowMenuAction: 鼠标当前位置: (${mouseLocation.x}, ${mouseLocation.y})")
                // 检查owner组件是否已显示
                if (owner.isShowing) {
                    try {
                        val ownerLocation = owner.locationOnScreen
                        val relativeX = mouseLocation.x - ownerLocation.x
                        val relativeY = mouseLocation.y - ownerLocation.y
                        println("WorkflowMenuAction: 在相对位置显示弹出菜单: ($relativeX, $relativeY)")
                        popup.show(owner, relativeX, relativeY)
                    } catch (e: java.awt.IllegalComponentStateException) {
                        println("WorkflowMenuAction: 坐标转换失败，在左上角显示: ${e.message}")
                        // 如果获取位置失败，在组件左上角显示
                        popup.show(owner, 0, 0)
                    }
                } else {
                    println("WorkflowMenuAction: owner未显示，在屏幕位置显示")
                    // 如果owner未显示，直接在屏幕位置显示
                    popup.show(null, mouseLocation.x, mouseLocation.y)
                }
            } else {
                println("WorkflowMenuAction: 无法获取鼠标位置或owner为空")
                // 最后的回退方案：在组件左上角或屏幕中央显示
                if (owner != null) {
                    println("WorkflowMenuAction: 在owner左上角显示弹出菜单")
                    popup.show(owner, 0, 0)
                } else {
                    println("WorkflowMenuAction: 在屏幕中央显示弹出菜单")
                    val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                    popup.show(null, screenSize.width / 2, screenSize.height / 2)
                }
            }
        }
        
        println("WorkflowMenuAction: 弹出菜单显示完成")
    }
    
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        // 根据任务状态设置不同的图标
        presentation.icon = when (taskStatus) {
            TaskStatus.PENDING -> com.intellij.icons.AllIcons.RunConfigurations.TestState.Run
            TaskStatus.IN_PROGRESS -> com.intellij.icons.AllIcons.Process.Step_1  // 使用加载/运行中的图标
            TaskStatus.COMPLETED -> com.intellij.icons.AllIcons.RunConfigurations.TestState.Run
        }
        
        // 设置悬停提示
        presentation.description = when (taskStatus) {
            TaskStatus.PENDING -> "待执行任务 - 点击查看可用操作"
            TaskStatus.IN_PROGRESS -> "进行中任务 - 点击查看可用操作"
            TaskStatus.COMPLETED -> "已完成任务 - 点击查看可用操作"
        }
    }
}