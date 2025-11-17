// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
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
class WorkflowLineMarkerProvider : LineMarkerProvider {
    companion object {
        // 用于跟踪已经处理过的行，防止重复创建行标记
        // 使用文件级别的映射，而不是全局映射
        private val fileProcessedLines = mutableMapOf<String, MutableSet<String>>()
        
        /**
         * 清理指定文件的处理记录
         */
        fun clearFileProcessedLines(filePath: String) {
            fileProcessedLines.remove(filePath)
        }
        
        /**
         * 获取指定文件的处理记录集合
         */
        private fun getFileProcessedLines(filePath: String): MutableSet<String> {
            return fileProcessedLines.getOrPut(filePath) { mutableSetOf() }
        }
    }
    
    init {
        // 创建消息总线连接
        val connection = com.intellij.openapi.application.ApplicationManager.getApplication().messageBus.connect()
        
        // 监听文件编辑器事件
        connection.subscribe(com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER, object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
            override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                if (file.name == "tasks.md" && file.path.contains("/.cospec/")) {
                    println("WorkflowLineMarkerProvider: 文件打开，清理文件处理记录 - ${file.path}")
                    clearFileProcessedLines(file.path)
                }
            }
            
            override fun fileClosed(source: com.intellij.openapi.fileEditor.FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                // 文件关闭时清理记录
                if (file.name == "tasks.md" && file.path.contains("/.cospec/")) {
                    println("WorkflowLineMarkerProvider: 文件关闭，清理文件处理记录 - ${file.path}")
                    clearFileProcessedLines(file.path)
                }
            }
        })
    }
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
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
        
        // 使用更简单但更可靠的方法：只处理特定类型的元素
        // 对于 Markdown 文件，我们只处理 PsiElement 类型为特定类的元素
        if (!isTargetElementType(element)) {
            return null
        }
        
        // 根据不同文件类型处理
        when (fileName) {
            "requirements.md" -> {
                // requirements.md 需求文档，为所有标题添加"同步变更至设计"按钮
                val elementText = element.text
                val lineStart = elementText.startsWith("#") || (elementText.trimStart().startsWith("#") && elementText.indexOf('#') < 5)
                
                if (lineStart) {
                    // 获取文档和行号，确保每行只处理一次
                    val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile)
                    if (document != null) {
                        val lineNumber = document.getLineNumber(element.textOffset)
                        val lineId = "$lineNumber"
                        val elementFilePath = element.containingFile.virtualFile.path
                        
                        // 获取当前文件的处理记录集合
                        val currentFileProcessedLines = getFileProcessedLines(elementFilePath)
                        
                        // 检查这一行是否已经被处理过
                        if (currentFileProcessedLines.contains(lineId)) {
                            return null
                        }
                        
                        // 标记这一行为已处理
                        currentFileProcessedLines.add(lineId)
                        
                        val syncToDesign = SyncToDesignAction("同步变更至设计")
                        return createLineMarkerInfo(element, AllIcons.Actions.CheckOut, "同步变更至设计", syncToDesign)
                    }
                }

                return null
            }
            
            "design.md" -> {
                // design.md 设计文档，为所有标题添加"同步变更至任务"按钮
                val elementText = element.text
                val lineStart = elementText.startsWith("#") || (elementText.trimStart().startsWith("#") && elementText.indexOf('#') < 5)
                
                if (lineStart) {
                    // 获取文档和行号，确保每行只处理一次
                    val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile)
                    if (document != null) {
                        val lineNumber = document.getLineNumber(element.textOffset)
                        val lineId = "$lineNumber"
                        val elementFilePath = element.containingFile.virtualFile.path
                        
                        // 获取当前文件的处理记录集合
                        val currentFileProcessedLines = getFileProcessedLines(elementFilePath)
                        
                        // 检查这一行是否已经被处理过
                        if (currentFileProcessedLines.contains(lineId)) {
                            return null
                        }
                        
                        // 标记这一行为已处理
                        currentFileProcessedLines.add(lineId)
                        
                        val syncToTasks = SyncToTasksAction("同步变更至任务")
                        return createLineMarkerInfo(element, AllIcons.Actions.CheckOut, "同步变更至任务", syncToTasks)
                    }
                }

                return null
            }
            
            "tasks.md" -> {
                // 确保文本不为空且包含内容
                if (text.isNullOrBlank()) {
                    return null
                }
                
                val trimmedText = text.trimStart()
                
                // tasks.md 任务列表文档，为每个任务项根据状态添加统一操作按钮
                // 检测任务项：以 "- [ ]"、"- [x]" 或 "- [-]" 开头的行
                if (trimmedText.startsWith("- [ ]") || // 待执行的任务
                    trimmedText.startsWith("- [x]") || // 已完成的任务
                    trimmedText.startsWith("- [-]")) { // 进行中的任务
                    
                    // 创建唯一标识符来跟踪已处理的行
                    val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile)
                    if (document != null) {
                        val lineNumber = document.getLineNumber(element.textOffset)
                        val lineId = "$lineNumber" // 只使用行号作为标识，因为现在是文件级别的映射
                        val elementFilePath = element.containingFile.virtualFile.path
                        
                        // 获取当前文件的处理记录集合
                        val currentFileProcessedLines = getFileProcessedLines(elementFilePath)
                        
                        // 检查这一行是否已经被处理过
                        if (currentFileProcessedLines.contains(lineId)) {
                            return null
                        }
                        
                        // 标记这一行为已处理
                        currentFileProcessedLines.add(lineId)
                    }
                    
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
                        
                        return createLineMarkerInfo(element, AllIcons.RunConfigurations.TestState.Run, statusText, menuAction)
                    }
                }

                return null // 如果不是任务项，则不添加标记
            }
        }

        return null
    }
    
    /**
     * 创建行标记信息
     */
    private fun createLineMarkerInfo(element: PsiElement, icon: javax.swing.Icon, tooltipText: String, action: AnAction): LineMarkerInfo<*> {
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltipText },
            { _, _ ->
                // 创建新的事件对象
                val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
                    "WorkflowLineMarker",
                    null,
                    com.intellij.openapi.actionSystem.DataContext { dataId ->
                        when (dataId) {
                            com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT.name -> element.project
                            com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT.name -> element
                            else -> null
                        }
                    }
                )
                action.actionPerformed(event)
            },
            com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment.CENTER,
            { tooltipText }
        )
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
        
        // 检查是否是任务行
        val isTaskLine = trimmedLineText.startsWith("- [") &&
            (trimmedLineText.startsWith("- [ ]") ||
             trimmedLineText.startsWith("- [x]") ||
             trimmedLineText.startsWith("- [-]"))
        
        // 如果是任务行，检查元素是否是任务标记的起始部分
        if (isTaskLine) {
            // 修复：更严格地检查元素是否是任务标记的开始部分
            // 只有当元素文本完全匹配 "- [" 开头时才认为是任务标记的开始
            if (elementText.startsWith("- [") && elementText.length >= 3) {
                // 进一步检查元素是否位于行首的非空白位置
                val elementOffsetInLine = element.textOffset - lineStartOffset
                val leadingWhitespace = fullLineText.take(elementOffsetInLine)
                
                // 只有当元素前面只有空白字符时才返回true
                val isAtLineStart = leadingWhitespace.isBlank()
                
                // 添加额外检查：确保元素是行中第一个包含 "- [" 的元素
                val textBeforeElement = fullLineText.take(elementOffsetInLine)
                val hasTaskMarkerBefore = textBeforeElement.contains("- [")
                
                return isAtLineStart && !hasTaskMarkerBefore
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
     * 检查元素是否是目标类型
     */
    private fun isTargetElementType(element: PsiElement): Boolean {
        // 对于 Markdown 文件，我们只处理特定类型的元素
        // 这可以避免同一行被多个元素处理
        val elementTypeName = element.javaClass.simpleName
        val elementText = element.text
        
        // 更精确的判断：只处理以 "- [" 开头的元素或以 "#" 开头的元素
        // 这样可以确保每个任务行或标题行只被处理一次
        val isTaskElement = elementText.trimStart().startsWith("- [")
        val isHeaderElement = elementText.trimStart().startsWith("#")
        
        // 只处理特定类型的元素，并且确保它们是任务项或标题
        return (isTaskElement || isHeaderElement) && (
               elementTypeName == "MarkdownListElement" ||
               elementTypeName == "MarkdownListItem" ||
               elementTypeName == "MarkdownTaskListItem" ||
               elementTypeName == "MarkdownParagraph" ||
               elementTypeName.contains("Markdown")
        )
    }
    
    /**
     * 检查当前任务是否是文档中的第一个任务块
     */
    private fun checkIfFirstTask(element: PsiElement): Boolean {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) ?: return false
        val currentLineNumber = document.getLineNumber(element.textOffset)
        
        
        // 使用与TaskLineBackgroundProvider相同的任务块解析逻辑
        val taskBlocks = parseTaskBlocks(document)

        // 检查当前行是否属于第一个任务块
        if (taskBlocks.isNotEmpty()) {
            val firstTaskBlock = taskBlocks[0]
            val isFirstTask = currentLineNumber >= firstTaskBlock.startLine && currentLineNumber <= firstTaskBlock.endLine
            return isFirstTask
        }
        
        return false
    }
    
    /**
     * 解析文档中的任务块（与TaskLineBackgroundProvider中的方法保持一致）
     */
    private fun parseTaskBlocks(document: com.intellij.openapi.editor.Document): List<TaskBlock> {
        val taskBlocks = mutableListOf<TaskBlock>()
        var currentTaskStartLine = -1
        var currentTaskStatus: String? = null
        
        // 任务模式
        val TASK_PATTERN = java.util.regex.Pattern.compile("^\\s*-\\s*\\[([ x-])\\]\\s+")
        
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
     * 判断一行是否是任务块的延续（与TaskLineBackgroundProvider中的方法保持一致）
     */
    private fun isTaskBlockContinuation(lineText: String): Boolean {
        // 先去除首尾空格
        val trimmedText = lineText.trim()
        
        // 空行不是任务块的延续
        if (trimmedText.isEmpty()) {
            return false
        }
        
        // 如果是新的任务项，则不是前一个任务块的延续
        val taskMatcher = java.util.regex.Pattern.compile("^\\s*-\\s*\\[([ x-])\\]\\s+").matcher(trimmedText)
        if (taskMatcher.find()) {
            return false
        }
        
        // Markdown 标题行不是子内容
        if (trimmedText.startsWith("#")) {
            return false
        }
        
        // 检查是否是缩进的内容（通常是任务块的子项或描述）
        val isIndented = lineText.startsWith(" ") || lineText.startsWith("\t")
        
        return isIndented
    }
    
    /**
     * 任务块数据类
     */
    data class TaskBlock(val startLine: Int, val endLine: Int, val status: String?)
}