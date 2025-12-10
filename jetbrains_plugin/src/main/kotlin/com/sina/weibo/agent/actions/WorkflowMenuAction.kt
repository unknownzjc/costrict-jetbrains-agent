
// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * 扩展函数：找到列表中第一个满足条件的元素及其索引
 */
fun <T> List<T>.findWithIndex(predicate: (T) -> Boolean): Pair<Int, T>? {
    for ((index, element) in this.withIndex()) {
        if (predicate(element)) {
            return Pair(index, element)
        }
    }
    return null
}

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
            com.intellij.openapi.ui.Messages.showErrorDialog("项目未正确初始化", "错误")
            return
        }
        
        // 检查操作可用性
        val availabilityResult = checkActionAvailability(e, project)
        if (!availabilityResult.isAvailable) {
            println("WorkflowMenuAction: 操作不可用 - ${availabilityResult.reason}")
            com.intellij.openapi.ui.Messages.showWarningDialog(
                project,
                availabilityResult.reason,
                "操作不可用"
            )
            return
        }
        
        val editor = availabilityResult.editor
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
                        val newAction = RunTaskAction()
                        val newEvent = createActionEvent(e, project, editor)
                        newAction.actionPerformed(newEvent)
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
                            val newAction = RunAllTasksAction()
                            val newEvent = createActionEvent(e, project, editor)
                            newAction.actionPerformed(newEvent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    // val generateTestsAction = JMenuItem("生成测试用例")
                    // generateTestsAction.addActionListener {
                    //     try {
                    //         val newAction = GenerateTestsAction()
                    //         val newEvent = createActionEvent(e, project, editor)
                    //         newAction.actionPerformed(newEvent)
                    //     } catch (ex: Exception) {
                    //         ex.printStackTrace()
                    //         println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                    //     }
                    // }
                    // popup.add(generateTestsAction)
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
                            val newAction = RunAllTasksAction()
                            val newEvent = createActionEvent(e, project, editor)
                            newAction.actionPerformed(newEvent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    // val generateTestsAction = JMenuItem("生成测试用例")
                    // generateTestsAction.addActionListener {
                    //     try {
                    //         val newAction = GenerateTestsAction()
                    //         val newEvent = createActionEvent(e, project, editor)
                    //         newAction.actionPerformed(newEvent)
                    //     } catch (ex: Exception) {
                    //         ex.printStackTrace()
                    //         println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                    //     }
                    // }
                    // popup.add(generateTestsAction)
                } else {
                     val retryAction = JMenuItem("重试")
                     retryAction.addActionListener {
                         try {
                             println("WorkflowMenuAction: 重试按钮被点击")
                             
                             // 尝试从事件上下文获取行号
                             val currentLineNumber = try {
                                 // 直接使用构造函数中传入的 taskText 来匹配任务块
                                 val document = editor?.document
                                 if (document != null) {
                                     // 使用 TaskBlockParser 找到当前点击的任务
                                     val taskBlocks = com.sina.weibo.agent.extensions.plugin.costrict.TaskBlockParser.parseTaskBlocks(document)
                                     println("WorkflowMenuAction: 找到 ${taskBlocks.size} 个任务块")
                                     println("WorkflowMenuAction: 当前任务文本: '$taskText'")
                                     
                                     val matchedBlock = taskBlocks.findWithIndex { block ->
                                         val startOffset = document.getLineStartOffset(block.startLine)
                                         val endOffset = document.getLineEndOffset(block.endLine)
                                         val blockText = document.getText(
                                             com.intellij.openapi.util.TextRange(startOffset, endOffset)
                                         )
                                         
                                         println("WorkflowMenuAction: 检查任务块，行范围 ${block.startLine}-${block.endLine}")
                                         println("WorkflowMenuAction: 任务块文本: '${blockText.trim()}'")
                                         
                                         // 检查任务文本是否匹配（使用多种匹配策略）
                                         val trimmedTaskText = taskText.trim()
                                         val trimmedBlockText = blockText.trim()
                                         
                                         trimmedBlockText.contains(trimmedTaskText) ||
                                         trimmedTaskText.contains(trimmedBlockText.take(50)) ||
                                         blockText.startsWith(taskText.trim()) ||
                                         taskText.trim().startsWith(blockText.trim().take(50))
                                     }
                                     
                                     println("WorkflowMenuAction: 匹配到的任务块索引: ${matchedBlock?.first}")
                                     matchedBlock?.second?.startLine
                                 } else {
                                     null
                                 }
                             } catch (ex: Exception) {
                                 println("WorkflowMenuAction: 从事件获取行号失败: ${ex.message}")
                                 ex.printStackTrace()
                                 null
                             } ?: run {
                                 // 回退到光标位置
                                 editor?.let {
                                     val caretModel = it.caretModel
                                     it.document.getLineNumber(caretModel.offset)
                                 }
                             }
                             
                             println("WorkflowMenuAction: 获取到行号: $currentLineNumber")
                             
                             // 创建带行号的 RetryTaskAction
                             val newAction = if (currentLineNumber != null) {
                                 RetryTaskAction(currentLineNumber)
                             } else {
                                 RetryTaskAction()
                             }
                             val newEvent = createActionEvent(e, project, editor)
                             newAction.actionPerformed(newEvent)
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
                       
                       // 尝试从事件上下文获取行号
                       val currentLineNumber = try {
                           // 直接使用构造函数中传入的 taskText 来匹配任务块
                           val document = editor?.document
                           if (document != null) {
                               // 使用 TaskBlockParser 找到当前点击的任务
                               val taskBlocks = com.sina.weibo.agent.extensions.plugin.costrict.TaskBlockParser.parseTaskBlocks(document)
                               println("WorkflowMenuAction: 找到 ${taskBlocks.size} 个任务块")
                               println("WorkflowMenuAction: 当前任务文本: '$taskText'")
                               
                               val matchedBlock = taskBlocks.findWithIndex { block ->
                                   val startOffset = document.getLineStartOffset(block.startLine)
                                   val endOffset = document.getLineEndOffset(block.endLine)
                                   val blockText = document.getText(
                                       com.intellij.openapi.util.TextRange(startOffset, endOffset)
                                   )
                                   
                                   println("WorkflowMenuAction: 检查任务块，行范围 ${block.startLine}-${block.endLine}")
                                   println("WorkflowMenuAction: 任务块文本: '${blockText.trim()}'")
                                   
                                   // 检查任务文本是否匹配（使用多种匹配策略）
                                   val trimmedTaskText = taskText.trim()
                                   val trimmedBlockText = blockText.trim()
                                   
                                   trimmedBlockText.contains(trimmedTaskText) ||
                                   trimmedTaskText.contains(trimmedBlockText.take(50)) ||
                                   blockText.startsWith(taskText.trim()) ||
                                   taskText.trim().startsWith(blockText.trim().take(50))
                               }
                               
                               println("WorkflowMenuAction: 匹配到的任务块索引: ${matchedBlock?.first}")
                               matchedBlock?.second?.startLine
                           } else {
                               null
                           }
                       } catch (ex: Exception) {
                           println("WorkflowMenuAction: 从事件获取行号失败: ${ex.message}")
                           ex.printStackTrace()
                           null
                       } ?: run {
                           // 回退到光标位置
                           editor?.let {
                               val caretModel = it.caretModel
                               it.document.getLineNumber(caretModel.offset)
                           }
                       }
                       
                       println("WorkflowMenuAction: 获取到行号: $currentLineNumber")
                       
                       // 创建带行号的 RetryTaskAction
                       val newAction = if (currentLineNumber != null) {
                           RetryTaskAction(currentLineNumber)
                       } else {
                           RetryTaskAction()
                       }
                       val newEvent = createActionEvent(e, project, editor)
                       newAction.actionPerformed(newEvent)
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
                            val newAction = RunAllTasksAction()
                            val newEvent = createActionEvent(e, project, editor)
                            newAction.actionPerformed(newEvent)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            println("WorkflowMenuAction: 运行所有任务失败 - ${ex.message}")
                        }
                    }
                    popup.add(runAllAction)
                    
                    // val generateTestsAction = JMenuItem("生成测试用例")
                    // generateTestsAction.addActionListener {
                    //     try {
                    //         println("WorkflowMenuAction: 生成测试用例按钮被点击")
                    //         val newAction = GenerateTestsAction()
                    //         val newEvent = createActionEvent(e, project, editor)
                    //         newAction.actionPerformed(newEvent)
                    //     } catch (ex: Exception) {
                    //         ex.printStackTrace()
                    //         println("WorkflowMenuAction: 生成测试用例失败 - ${ex.message}")
                    //     }
                    // }
                    // popup.add(generateTestsAction)
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
    
    /**
     * 创建包含正确编辑器上下文的 AnActionEvent
     */
    private fun createActionEvent(
        originalEvent: AnActionEvent,
        project: com.intellij.openapi.project.Project,
        editor: com.intellij.openapi.editor.Editor?
    ): AnActionEvent {
        println("WorkflowMenuAction: 开始创建 ActionEvent，编辑器: ${editor?.javaClass?.simpleName}")
        
        // 确保获取到有效的编辑器
        val effectiveEditor = editor ?: run {
            println("WorkflowMenuAction: 传入编辑器为空，尝试从 FileEditorManager 获取")
            try {
                val selectedEditor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
                println("WorkflowMenuAction: 从 FileEditorManager 获取到编辑器: ${selectedEditor?.javaClass?.simpleName}")
                selectedEditor
            } catch (ex: Exception) {
                println("WorkflowMenuAction: 从 FileEditorManager 获取编辑器失败: ${ex.message}")
                null
            }
        }
        
        // 预先计算所有上下文数据，确保数据有效性
        val psiFile = effectiveEditor?.let { ed ->
            try {
                val psi = PsiDocumentManager.getInstance(project).getPsiFile(ed.document)
                println("WorkflowMenuAction: 获取到 PSI 文件: ${psi?.name}")
                psi
            } catch (ex: Exception) {
                println("WorkflowMenuAction: 获取 PSI 文件失败: ${ex.message}")
                null
            }
        }
        
        val virtualFile = effectiveEditor?.let { ed ->
            try {
                val vFile = FileDocumentManager.getInstance().getFile(ed.document)
                println("WorkflowMenuAction: 获取到虚拟文件: ${vFile?.name}")
                vFile
            } catch (ex: Exception) {
                println("WorkflowMenuAction: 获取虚拟文件失败: ${ex.message}")
                null
            }
        }
        
        // 验证获取的数据
        println("WorkflowMenuAction: 上下文验证 - Editor: ${effectiveEditor != null}, PSI: ${psiFile != null}, VFile: ${virtualFile != null}")
        
        // 创建新的数据上下文，确保包含所有必要的信息
        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> {
                    println("WorkflowMenuAction: 请求 PROJECT 数据")
                    project
                }
                CommonDataKeys.EDITOR.name -> {
                    println("WorkflowMenuAction: 请求 EDITOR 数据: ${effectiveEditor != null}")
                    effectiveEditor
                }
                CommonDataKeys.PSI_FILE.name -> {
                    println("WorkflowMenuAction: 请求 PSI_FILE 数据: ${psiFile != null}")
                    psiFile
                }
                CommonDataKeys.VIRTUAL_FILE.name -> {
                    println("WorkflowMenuAction: 请求 VIRTUAL_FILE 数据: ${virtualFile != null}")
                    virtualFile
                }
                else -> {
                    val originalData = originalEvent.dataContext.getData(dataId)
                    println("WorkflowMenuAction: 请求其他数据 '$dataId': ${originalData != null}")
                    originalData
                }
            }
        }
        
        // 创建新的 AnActionEvent
        val newEvent = AnActionEvent(
            originalEvent.inputEvent,
            dataContext,
            originalEvent.place,
            originalEvent.presentation,
            ActionManager.getInstance(),
            originalEvent.modifiers
        )
        
        println("WorkflowMenuAction: ActionEvent 创建完成")
        return newEvent
    }
    
    /**
     * 操作可用性检查结果
     */
    private data class ActionAvailabilityResult(
        val isAvailable: Boolean,
        val editor: com.intellij.openapi.editor.Editor?,
        val reason: String = ""
    )
    
    /**
     * 检查操作可用性
     */
    private fun checkActionAvailability(
        e: AnActionEvent,
        project: com.intellij.openapi.project.Project
    ): ActionAvailabilityResult {
        println("WorkflowMenuAction: 开始检查操作可用性")
        
        // 检查是否有打开的编辑器
        val editor = try {
            com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
        } catch (ex: Exception) {
            println("WorkflowMenuAction: 获取编辑器失败 - ${ex.message}")
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = null,
                reason = "无法获取当前编辑器，请确保有文件处于编辑状态"
            )
        }
        
        if (editor == null) {
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = null,
                reason = "当前没有打开的编辑器，请打开一个支持的文件"
            )
        }
        
        // 检查文件类型
        val virtualFile = try {
            FileDocumentManager.getInstance().getFile(editor.document)
        } catch (ex: Exception) {
            println("WorkflowMenuAction: 获取虚拟文件失败 - ${ex.message}")
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = editor,
                reason = "无法获取文件信息，请重新打开文件"
            )
        }
        
        if (virtualFile == null) {
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = editor,
                reason = "当前编辑器没有关联的文件"
            )
        }
        
        // 检查是否为支持的 Costrict 文件
        val supportedFiles = com.sina.weibo.agent.extensions.plugin.costrict.CostrictFileConstants.SUPPORTED_FILES
        if (virtualFile.name !in supportedFiles) {
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = editor,
                reason = "当前文件类型不受支持。支持的文件类型：${supportedFiles.joinToString(", ")}"
            )
        }
        
        // 检查是否在 .cospec 目录中
        val cospecDir = com.sina.weibo.agent.extensions.plugin.costrict.CostrictFileConstants.COSPEC_DIR
        if (!virtualFile.path.contains(cospecDir)) {
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = editor,
                reason = "文件必须位于 $cospecDir 目录中"
            )
        }
        
        // 检查文件是否可读写
        if (!virtualFile.isWritable) {
            return ActionAvailabilityResult(
                isAvailable = false,
                editor = editor,
                reason = "当前文件为只读状态，无法执行操作"
            )
        }
        
        println("WorkflowMenuAction: 操作可用性检查通过")
        return ActionAvailabilityResult(
            isAvailable = true,
            editor = editor
        )
    }
    
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        
        // 检查操作可用性并更新 UI 状态
        val project = e.project
        if (project != null) {
            val availabilityResult = checkActionAvailability(e, project)
            presentation.isEnabled = availabilityResult.isAvailable
            
            if (!availabilityResult.isAvailable) {
                presentation.description = availabilityResult.reason
                presentation.icon = com.intellij.icons.AllIcons.General.Warning
                return
            }
        }
        
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