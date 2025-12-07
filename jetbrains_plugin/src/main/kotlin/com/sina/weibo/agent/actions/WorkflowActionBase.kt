// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDocumentManager
import com.sina.weibo.agent.extensions.plugin.costrict.CostrictFileConstants
import com.sina.weibo.agent.extensions.plugin.costrict.TaskBlockParser

/**
 * Workflow Action 基类
 * 提供参数收集、RPC 调用、错误处理等通用功能
 */
abstract class WorkflowActionBase(
    private val actionName: String,
    private val rpcCommand: String,
    private val actionType: String
) : AnAction(actionName) {
    
    protected val logger: Logger = Logger.getInstance(javaClass)
    
    /**
     * 编辑器上下文结果
     */
    private data class EditorContextResult(
        val isValid: Boolean,
        val editor: Editor?,
        val psiFile: PsiFile?,
        val virtualFile: VirtualFile?,
        val errorMessage: String = ""
    )
    
    override fun actionPerformed(e: AnActionEvent) {
        logger.info("$actionName: 开始执行 actionPerformed")
        
        val project = e.project
        if (project == null) {
            logger.warn("$actionName: 项目为空，返回")
            Messages.showErrorDialog("项目为空，无法执行操作", "错误")
            return
        }
        
        // 尝试获取编辑器上下文，使用多种策略确保成功
        val contextResult = getEditorContext(e, project)
        if (!contextResult.isValid) {
            logger.warn("$actionName: ${contextResult.errorMessage}")
            Messages.showErrorDialog(contextResult.errorMessage, "错误")
            return
        }
        
        val editor = contextResult.editor!!
        val psiFile = contextResult.psiFile!!
        val virtualFile = contextResult.virtualFile!!
        
        try {
            // 收集参数
            val params = collectParams(project, editor, psiFile, virtualFile)
            
            // 验证参数
            val validationResult = WorkflowActionParams.validate(params)
            if (!validationResult.isValid) {
                logger.error("$actionName: 参数验证失败 - ${validationResult.errorMessage}")
                Messages.showErrorDialog("参数验证失败: ${validationResult.errorMessage}", "错误")
                return
            }
            
            // 执行 RPC 调用
            executeWorkflowCommand(project, params)
            
        } catch (ex: Exception) {
            logger.error("$actionName: 执行过程中发生错误", ex)
            Messages.showErrorDialog("执行失败: ${ex.message}", "错误")
        }
    }
    
    /**
     * 收集 Workflow 参数
     * 子类可以重写此方法以提供特定的参数收集逻辑
     */
    protected open fun collectParams(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        virtualFile: VirtualFile
    ): WorkflowActionParams {
        val document = editor.document
        val caretModel = editor.caretModel
        val lineNumber = document.getLineNumber(caretModel.offset)
        
        // 基础参数
        val builder = WorkflowActionParams(
            filePath = virtualFile.path,
            documentType = getDocumentType(virtualFile.name),
            lineNumber = lineNumber,
            actionType = actionType
        )
        
        // 获取选中文本
        val selectionModel = editor.selectionModel
        return if (selectionModel.hasSelection()) {
            builder.copy(
                selectedText = selectionModel.selectedText,
                startLine = document.getLineNumber(selectionModel.selectionStart),
                endLine = document.getLineNumber(selectionModel.selectionEnd)
            )
        } else {
            builder
        }
    }
    
    /**
     * 执行 Workflow RPC 命令
     */
    protected fun executeWorkflowCommand(project: Project, params: WorkflowActionParams) {
        logger.info("$actionName: 执行 RPC 命令 - $rpcCommand")
        
        // 显示进度提示
        // Messages.showInfoMessage(project, "正在执行 $actionName...", "执行中")
        
        // 调用 RPC
        val paramsMap = WorkflowActionParams.toMap(params)
        executeCommand(
            rpcCommand,
            project,
            paramsMap,
            hasArgs = true
        )
        
        logger.info("$actionName: RPC 命令已发送")
    }
    
    /**
     * 获取文档类型
     */
    protected fun getDocumentType(fileName: String): String {
        return when (fileName) {
            CostrictFileConstants.REQUIREMENTS_FILE -> "requirements"
            CostrictFileConstants.DESIGN_FILE -> "design"
            CostrictFileConstants.TASKS_FILE -> "tasks"
            else -> "unknown"
        }
    }
    
    /**
     * 检查是否为支持的 Costrict 文件
     */
    protected fun isSupportedCospecFile(virtualFile: VirtualFile): Boolean {
        return virtualFile.name in CostrictFileConstants.SUPPORTED_FILES &&
               virtualFile.path.contains(CostrictFileConstants.COSPEC_DIR)
    }
    
    /**
     * 获取任务文本（用于 tasks.md）
     * @param lineNumber 1-based 行号
     */
    protected fun getTaskText(editor: Editor, psiFile: PsiFile, lineNumber: Int): String? {
        if (!psiFile.name.endsWith(CostrictFileConstants.TASKS_FILE)) {
            return null
        }
        
        val document = editor.document
        val taskBlocks = TaskBlockParser.parseTaskBlocks(document)
        
        // 将 1-based lineNumber 转换为 0-based 进行匹配
        val zeroBasedLineNumber = lineNumber - 1
        
        val currentTaskBlock = taskBlocks.find { block ->
            zeroBasedLineNumber in block.startLine..block.endLine
        }
        
        return currentTaskBlock?.let { block ->
            val startOffset = document.getLineStartOffset(block.startLine)
            val endOffset = document.getLineEndOffset(block.endLine)
            document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        }
    }
    
    /**
     * 获取整个文件内容
     */
    protected fun getFileContent(editor: Editor): String {
        return editor.document.text
    }
    
    /**
     * 获取编辑器上下文，使用多种策略确保成功
     */
    private fun getEditorContext(e: AnActionEvent, project: Project): EditorContextResult {
        var editor = e.getData(CommonDataKeys.EDITOR)
        var psiFile = e.getData(CommonDataKeys.PSI_FILE)
        var virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        val missingComponents = mutableListOf<String>()
        
        // 策略1：直接从事件数据上下文获取
        if (editor == null) missingComponents.add("Editor")
        if (psiFile == null) missingComponents.add("PSI File")
        if (virtualFile == null) missingComponents.add("Virtual File")
        
        // 策略2：如果直接获取失败，尝试从 FileEditorManager 获取
        if (editor == null) {
            try {
                editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
                if (editor != null) {
                    logger.info("$actionName: 通过 FileEditorManager 成功获取编辑器")
                    missingComponents.remove("Editor")
                }
            } catch (ex: Exception) {
                logger.warn("$actionName: 通过 FileEditorManager 获取编辑器失败", ex)
            }
        }
        
        // 策略3：如果有编辑器但缺少其他组件，尝试从编辑器推导
        if (editor != null) {
            if (virtualFile == null) {
                try {
                    virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
                    if (virtualFile != null) {
                        logger.info("$actionName: 通过编辑器文档成功获取虚拟文件")
                        missingComponents.remove("Virtual File")
                    }
                } catch (ex: Exception) {
                    logger.warn("$actionName: 通过编辑器文档获取虚拟文件失败", ex)
                }
            }
            
            if (psiFile == null) {
                try {
                    psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
                    if (psiFile != null) {
                        logger.info("$actionName: 通过编辑器文档成功获取PSI文件")
                        missingComponents.remove("PSI File")
                    }
                } catch (ex: Exception) {
                    logger.warn("$actionName: 通过编辑器文档获取PSI文件失败", ex)
                }
            }
        }
        
        // 策略4：检查文件类型是否支持
        if (virtualFile != null && !isSupportedCospecFile(virtualFile)) {
            return EditorContextResult(
                isValid = false,
                editor = editor,
                psiFile = psiFile,
                virtualFile = virtualFile,
                errorMessage = "当前文件不是支持的 Costrict 文件类型。支持的文件：${CostrictFileConstants.SUPPORTED_FILES.joinToString(", ")}"
            )
        }
        
        // 最终检查
        val stillMissingComponents = mutableListOf<String>()
        if (editor == null) stillMissingComponents.add("Editor")
        if (psiFile == null) stillMissingComponents.add("PSI File")
        if (virtualFile == null) stillMissingComponents.add("Virtual File")
        
        return if (stillMissingComponents.isEmpty()) {
            logger.info("$actionName: 成功获取所有编辑器上下文组件")
            EditorContextResult(
                isValid = true,
                editor = editor,
                psiFile = psiFile,
                virtualFile = virtualFile
            )
        } else {
            val errorMsg = "无法获取编辑器上下文，缺失组件: ${stillMissingComponents.joinToString(", ")}。" +
                    "请确保在支持的文件中打开编辑器并聚焦到文档上。"
            logger.error("$actionName: $errorMsg")
            EditorContextResult(
                isValid = false,
                editor = editor,
                psiFile = psiFile,
                virtualFile = virtualFile,
                errorMessage = errorMsg
            )
        }
    }
    
    /**
     * 获取任务状态
     */
    protected fun getTaskStatus(lineText: String): String? {
        return when {
            lineText.contains(CostrictFileConstants.TASK_PENDING) -> "pending"
            lineText.contains(CostrictFileConstants.TASK_IN_PROGRESS) -> "in_progress"
            lineText.contains(CostrictFileConstants.TASK_COMPLETED) -> "completed"
            else -> null
        }
    }
    
    /**
     * 检查是否为第一个任务
     */
    protected fun isFirstTask(editor: Editor, psiFile: PsiFile, lineNumber: Int): Boolean {
        if (!psiFile.name.endsWith(CostrictFileConstants.TASKS_FILE)) {
            return false
        }
        
        val document = editor.document
        val taskBlocks = TaskBlockParser.parseTaskBlocks(document)
        
        // 将 1-based lineNumber 转换为 0-based 进行匹配
        val zeroBasedLineNumber = lineNumber - 1
        
        // 检查指定行号是否在第一个任务块内
        val firstTaskBlock = taskBlocks.firstOrNull()
        return firstTaskBlock?.let { block ->
            zeroBasedLineNumber in block.startLine..block.endLine
        } ?: false
    }
}