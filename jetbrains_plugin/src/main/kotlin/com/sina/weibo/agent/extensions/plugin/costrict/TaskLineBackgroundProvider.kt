// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.JBColor
import com.intellij.util.messages.MessageBusConnection
import java.awt.Color
import java.awt.Font
import java.util.regex.Pattern

/**
 * 任务行背景提供者
 * 为 .cospec 目录下的 tasks.md 文件中的任务项设置不同背景颜色
 */
class TaskLineBackgroundProvider {
    companion object {
        // 颜色定义
        private val DEFAULT_BACKGROUND = Color(255, 255, 255, 0) // 透明背景
        private val IN_PROGRESS_BACKGROUND = Color(255, 255, 200, 80) // 浅黄色背景
        private val COMPLETED_BACKGROUND = Color(200, 255, 200, 80) // 浅绿色背景
        
        // 任务模式
        private val TASK_PATTERN = Pattern.compile("^\\s*-\\s*\\[([ x-])\\]\\s+")
        
        // 存储编辑器和高亮器的映射
        private val editorHighlighters = mutableMapOf<Editor, MutableList<RangeHighlighter>>()
    }
    
    private var connection: MessageBusConnection? = null
    
    /**
     * 初始化提供者
     */
    fun init() {
        println("TaskLineBackgroundProvider: 开始初始化")
        
        // 订阅文件编辑器事件
        connection = ApplicationManager.getApplication().messageBus.connect()
        
        // 监听文件打开事件
        connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                println("TaskLineBackgroundProvider: 文件打开事件 - ${file.path}")
                if (isTasksFile(file)) {
                    println("TaskLineBackgroundProvider: 识别为 tasks.md 文件，开始更新背景")
                    updateBackgrounds(source.project, file)
                } else {
                    println("TaskLineBackgroundProvider: 不是 tasks.md 文件，跳过")
                }
            }
            
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                println("TaskLineBackgroundProvider: 文件关闭事件 - ${file.path}")
                if (isTasksFile(file)) {
                    clearBackgrounds(source, file)
                }
            }
        })
        
        // 监听文件内容变化
        connection?.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    if (event is VFileContentChangeEvent) {
                        val file = event.file
                        println("TaskLineBackgroundProvider: 文件内容变化事件 - ${file.path}")
                        if (isTasksFile(file)) {
                            println("TaskLineBackgroundProvider: 识别为 tasks.md 文件，开始更新背景")
                            // 遍历所有项目，找到包含该文件的项目
                            ProjectManager.getInstance().openProjects.forEach { project ->
                                ApplicationManager.getApplication().invokeLater {
                                    // 修复：获取所有打开该文件的编辑器，而不仅仅是选中的编辑器
                                    FileEditorManager.getInstance(project).allEditors.forEach { editor ->
                                        if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                                            val editorFile = FileDocumentManager.getInstance().getFile(editor.editor.document)
                                            if (editorFile == file) {
                                                updateBackgroundsForEditor(editor.editor, file)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            println("TaskLineBackgroundProvider: 不是 tasks.md 文件，跳过")
                        }
                    }
                }
            }
        })
        
        println("TaskLineBackgroundProvider: 初始化完成")
    }
    
    /**
     * 清理资源
     */
    fun dispose() {
        println("TaskLineBackgroundProvider: 开始清理资源")
        connection?.disconnect()
        connection = null
        
        // 清理所有高亮器
        editorHighlighters.values.forEach { highlighters ->
            highlighters.forEach { highlighter ->
                highlighter.dispose()
            }
        }
        editorHighlighters.clear()
        println("TaskLineBackgroundProvider: 资源清理完成")
    }
    
    /**
     * 检查是否为 tasks.md 文件
     */
    private fun isTasksFile(file: VirtualFile): Boolean {
        val result = file.name == "tasks.md" && file.path.contains("/.cospec/")
        println("TaskLineBackgroundProvider: 文件检查 - ${file.path}，是否为 tasks.md: ${file.name == "tasks.md"}，是否在 .cospec 目录: ${file.path.contains("/.cospec/")}，结果: $result")
        return result
    }
    
    /**
     * 更新任务行背景
     */
    private fun updateBackgrounds(project: Project, file: VirtualFile) {
        // 修复：获取所有打开该文件的编辑器，而不仅仅是选中的编辑器
        FileEditorManager.getInstance(project).allEditors.forEach { editor ->
            if (editor is com.intellij.openapi.fileEditor.TextEditor) {
                val editorFile = FileDocumentManager.getInstance().getFile(editor.editor.document)
                if (editorFile == file) {
                    updateBackgroundsForEditor(editor.editor, file)
                }
            }
        }
    }
    
    /**
     * 为特定编辑器更新背景
     */
    private fun updateBackgroundsForEditor(editor: Editor, file: VirtualFile) {
        ApplicationManager.getApplication().runReadAction {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction
            
            // 添加日志：调试信息
            println("TaskLineBackgroundProvider: 开始处理文件 ${file.path}")
            println("TaskLineBackgroundProvider: 文档行数 ${document.lineCount}")
            println("TaskLineBackgroundProvider: 编辑器实例 ${editor.javaClass.simpleName}")
            
            // 清除之前的高亮器
            clearBackgroundsForEditor(editor)
            
            // 修复：使用改进的任务块解析逻辑
            val taskBlocks = parseTaskBlocks(document)
            var taskBlockCount = 0
            
            for (taskBlock in taskBlocks) {
                createTaskBlockHighlighter(editor, document, taskBlock.startLine, taskBlock.endLine, taskBlock.status)
                taskBlockCount++
                println("TaskLineBackgroundProvider: 创建任务块 ${taskBlockCount}，行 ${taskBlock.startLine}-${taskBlock.endLine}，状态 ${taskBlock.status}")
            }
            
            println("TaskLineBackgroundProvider: 处理完成，共识别 $taskBlockCount 个任务块")
        }
    }
    
    /**
     * 任务块数据类
     */
    data class TaskBlock(val startLine: Int, val endLine: Int, val status: String?)
    
    /**
     * 解析文档中的任务块
     */
    private fun parseTaskBlocks(document: com.intellij.openapi.editor.Document): List<TaskBlock> {
        val taskBlocks = mutableListOf<TaskBlock>()
        var currentTaskStartLine = -1
        var currentTaskStatus: String? = null
        
        for (line in 0 until document.lineCount) {
            val startOffset = document.getLineStartOffset(line)
            val endOffset = document.getLineEndOffset(line)
            val lineText = document.getText(
                com.intellij.openapi.util.TextRange(startOffset, endOffset)
            )
            
            // 检查是否是任务行
            val matcher = TASK_PATTERN.matcher(lineText.trim())
            if (matcher.find()) {
                // 如果已经开始了一个任务块，先结束前一个任务块
                if (currentTaskStartLine != -1) {
                    taskBlocks.add(TaskBlock(currentTaskStartLine, line - 1, currentTaskStatus))
                }
                
                // 开始新的任务块
                currentTaskStartLine = line
                currentTaskStatus = matcher.group(1).lowercase()
            } else if (currentTaskStartLine != -1) {
                // 检查当前行是否属于任务块的延续
                val isContinuation = isTaskBlockContinuation(lineText)
                val isEmptyLine = lineText.trim().isEmpty()
                
                println("TaskLineBackgroundProvider: 行 $line 文本: '$lineText'")
                println("TaskLineBackgroundProvider: 是否延续: $isContinuation, 是否空行: $isEmptyLine")
                
                // 如果不是任务块的延续或者是空行，结束当前任务块
                if (!isContinuation || isEmptyLine) {
                    taskBlocks.add(TaskBlock(currentTaskStartLine, line - 1, currentTaskStatus))
                    currentTaskStartLine = -1
                    currentTaskStatus = null
                }
            }
        }
        
        // 处理文档末尾的任务块
        if (currentTaskStartLine != -1) {
            taskBlocks.add(TaskBlock(currentTaskStartLine, document.lineCount - 1, currentTaskStatus))
        }
        
        return taskBlocks
    }
    
    /**
     * 判断一行是否是任务块的延续
     */
    private fun isTaskBlockContinuation(lineText: String): Boolean {
        // 先去除首尾空格
        val trimmedText = lineText.trim()
        
        // 空行不是任务块的延续
        if (trimmedText.isEmpty()) {
            println("TaskLineBackgroundProvider: 行为空，不是任务块延续")
            return false
        }
        
        // 如果是新的任务项，则不是前一个任务块的延续
        val taskMatcher = TASK_PATTERN.matcher(trimmedText)
        if (taskMatcher.find()) {
            println("TaskLineBackgroundProvider: 发现新任务项，不是前一个任务块的延续")
            return false
        }
        
        // Markdown 标题行不是子内容
        if (trimmedText.startsWith("#")) {
            println("TaskLineBackgroundProvider: 发现 Markdown 标题，不是任务块延续")
            return false
        }
        
        // 修复：改进的任务块延续逻辑
        // 检查是否是缩进的内容（通常是任务块的子项或描述）
        val isIndented = lineText.startsWith(" ") || lineText.startsWith("\t")
        
        // 检查是否是列表项（但不是任务项）
        val isListItem = trimmedText.matches(Regex("^[-*+]\\s+.*"))
        
        // 检查是否是数字列表项
        val isNumberedListItem = trimmedText.matches(Regex("^\\d+\\.\\s+.*"))
        
        // 修复：改进逻辑 - 任何缩进的内容都视为任务块的延续，包括普通文本
        val result = isIndented
        
        // 添加详细日志
        println("TaskLineBackgroundProvider: 任务块延续判断:")
        println("  原始文本: '$lineText'")
        println("  去除空格后: '$trimmedText'")
        println("  是否缩进: $isIndented")
        println("  是否列表项: $isListItem")
        println("  是否数字列表项: $isNumberedListItem")
        println("  最终结果: $result")
        
        return result
    }
    
    /**
     * 为任务块创建高亮器
     */
    private fun createTaskBlockHighlighter(editor: Editor, document: com.intellij.openapi.editor.Document,
                                         startLine: Int, endLine: Int, status: String?) {
        // 修复：改进范围计算
        val startOffset = document.getLineStartOffset(startLine)
        val endOffset = if (endLine >= startLine && endLine < document.lineCount) {
            // 获取最后一行的完整内容，包括行尾
            document.getLineEndOffset(endLine)
        } else {
            startOffset
        }
        
        // 添加日志：调试范围计算
        println("TaskLineBackgroundProvider: 创建高亮器 - 行 $startLine-$endLine，偏移 $startOffset-$endOffset，状态 $status")
        
        // 根据任务状态确定背景色
        val color = when (status) {
            "-" -> IN_PROGRESS_BACKGROUND // 进行中：黄色背景
            "x" -> COMPLETED_BACKGROUND // 已完成：绿色背景
            else -> DEFAULT_BACKGROUND // 默认：透明背景
        }
        
        println("TaskLineBackgroundProvider: 背景色 $color")
        
        // 修复：使用 WHOLE_LINE 而不是 EXACT_RANGE 来确保整行高亮
        val markupModel = editor.markupModel
        val textAttributes = TextAttributes(null, color, null, null, Font.PLAIN)
        val highlighter = markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION - 1, // 确保在其他高亮之下
            textAttributes,
            com.intellij.openapi.editor.markup.HighlighterTargetArea.LINES_IN_RANGE // 修复：使用正确的枚举值
        )
        
        // 存储高亮器以便后续清理
        editorHighlighters.getOrPut(editor) { mutableListOf() }.add(highlighter)
        
        println("TaskLineBackgroundProvider: 高亮器已创建并添加，当前编辑器共有 ${editorHighlighters[editor]?.size} 个高亮器")
    }
    
    /**
     * 清除指定编辑器的背景高亮
     */
    private fun clearBackgroundsForEditor(editor: Editor) {
        val highlighters = editorHighlighters[editor] ?: return
        println("TaskLineBackgroundProvider: 清理编辑器的 ${highlighters.size} 个高亮器")
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
    
    /**
     * 清除文件的所有背景高亮
     */
    private fun clearBackgrounds(source: FileEditorManager, file: VirtualFile) {
        source.allEditors.forEach { fileEditor ->
            if (fileEditor is com.intellij.openapi.fileEditor.TextEditor) {
                val editorFile = FileDocumentManager.getInstance().getFile(fileEditor.editor.document)
                if (editorFile == file) {
                    clearBackgroundsForEditor(fileEditor.editor)
                }
            }
        }
    }
}