// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement

/**
 * Costrict extension provider implementation
 * 为 .cospec 目录下的 requirements.md、design.md 和 tasks.md 文件添加行标记按钮
 */
class WorkflowLineMarkerProvider : LineMarkerProvider {
    companion object {
        /**
         * 文件级别的缓存映射，用于跟踪每个文件中已处理的行
         * 键：文件绝对路径，值：已处理的行号集合
         */
        private val fileProcessedLinesCache = mutableMapOf<String, MutableSet<String>>()
        
        /**
         * 清理指定文件的缓存
         */
        fun clearFileCache(filePath: String) {
            fileProcessedLinesCache.remove(filePath)
        }
        
        /**
         * 清理所有缓存
         */
        fun clearAllCache() {
            fileProcessedLinesCache.clear()
        }
        
        /**
         * 打印当前缓存状态（调试用）
         */
        fun printCacheState() {
            println("WorkflowLineMarkerProvider: 当前缓存状态 - 文件数: ${fileProcessedLinesCache.size}")
            fileProcessedLinesCache.forEach { (filePath, lines) ->
                println("  - $filePath: ${lines.size} 行")
            }
        }
        
        /**
         * 获取指定文件的已处理行集合，如果不存在则创建
         */
        private fun getFileProcessedLines(filePath: String): MutableSet<String> {
            return fileProcessedLinesCache.getOrPut(filePath) {
                mutableSetOf()
            }
        }
    }
    
    private val handlers: List<FileTypeHandler> = listOf(
        RequirementsFileHandler(),
        DesignFileHandler(),
        TasksFileHandler()
    )
    
    init {
        // 创建消息总线连接
        val connection = com.intellij.openapi.application.ApplicationManager.getApplication().messageBus.connect()
        
        // 监听文件编辑器事件
        connection.subscribe(com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER, object : com.intellij.openapi.fileEditor.FileEditorManagerListener {
            // override fun fileOpened(source: com.intellij.openapi.fileEditor.FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
            //     if (file.name in CostrictFileConstants.SUPPORTED_FILES && file.path.contains(CostrictFileConstants.COSPEC_DIR)) {
            //         println("WorkflowLineMarkerProvider: 文件打开 - ${file.path}")
            //         printCacheState()
            //     }
            // }
            
            override fun fileClosed(source: com.intellij.openapi.fileEditor.FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
                // 文件关闭时清理该文件的缓存
                if (file.name in CostrictFileConstants.SUPPORTED_FILES && file.path.contains(CostrictFileConstants.COSPEC_DIR)) {
                    // println("WorkflowLineMarkerProvider: 文件关闭 - ${file.path}")
                    clearFileCache(file.path)
                    // printCacheState()
                }
            }
            
            override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                val oldFile = event.oldFile
                // val newFile = event.newFile
                
                if (oldFile != null && oldFile.name in CostrictFileConstants.SUPPORTED_FILES && oldFile.path.contains(CostrictFileConstants.COSPEC_DIR)) {
                    // println("WorkflowLineMarkerProvider: 从文件切换出 - ${oldFile.path}")
                    // 切换出时清理缓存，允许下次切换回来时重新创建标记
                    clearFileCache(oldFile.path)
                }
                
                // if (newFile != null && newFile.name in CostrictFileConstants.SUPPORTED_FILES && newFile.path.contains(CostrictFileConstants.COSPEC_DIR)) {
                //     // println("WorkflowLineMarkerProvider: 切换到文件 - ${newFile.path}")
                //     // printCacheState()
                // }
            }
        })
    }
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!isInCospecDirectory(element)) return null
        
        val file = element.containingFile?.virtualFile ?: return null
        if (!isSupportedCospecFile(file)) return null
        
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return null
        
        val filePath = file.path
        val lineNumber = document.getLineNumber(element.textOffset)
        val lineId = "$lineNumber"
        
        // 获取当前文件的已处理行集合
        val processedLines = getFileProcessedLines(filePath)
        
        // 检查这行是否已经被处理过
        if (processedLines.contains(lineId)) {
            return null
        }
        
        val marker = handlers.firstOrNull { it.canHandle(file.name) }
            ?.takeIf { it.shouldProcessElement(element) }
            ?.createLineMarker(element, document)
        
        // 如果成功创建了标记，记录这行已处理
        if (marker != null) {
            processedLines.add(lineId)
        }
        
        return marker
    }
    
    /**
     * 检查元素是否在 .cospec 目录下
     */
    private fun isInCospecDirectory(element: PsiElement): Boolean {
        return element.containingFile?.virtualFile?.path
            ?.contains(CostrictFileConstants.COSPEC_DIR) == true
    }
    
    /**
     * 检查文件是否支持的 .cospec 文件
     */
    private fun isSupportedCospecFile(file: com.intellij.openapi.vfs.VirtualFile): Boolean {
        return file.name in CostrictFileConstants.SUPPORTED_FILES &&
               file.path.contains(CostrictFileConstants.COSPEC_DIR)
    }
}