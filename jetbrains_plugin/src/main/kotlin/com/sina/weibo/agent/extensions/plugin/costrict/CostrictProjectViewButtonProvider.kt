// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiElement
import com.sina.weibo.agent.actions.executeCommand
import com.sina.weibo.agent.extensions.ui.buttons.ExtensionButtonProvider
import com.sina.weibo.agent.extensions.ui.buttons.ButtonType
import com.sina.weibo.agent.extensions.ui.buttons.ButtonConfiguration

/**
 * Costrict project view button provider.
 * Provides button configuration specific to Costrict extension for project view operations.
 * Currently provides code review functionality for files only (directories are not supported).
 */
class CostrictProjectViewButtonProvider : ExtensionButtonProvider {
    
    override fun getExtensionId(): String = "costrict"
    
    override fun getDisplayName(): String = "Costrict"
    
    override fun getDescription(): String = "AI-powered code assistant for project view operations"
    
    override fun isAvailable(project: Project): Boolean {
        // Check if costrict extension is available
        return true
    }
    
    override fun getButtons(project: Project): List<AnAction> {
        // Currently only provide code review action for project view
        return listOf(
            CodeReviewAction()
        )
    }
    
    override fun getButtonConfiguration(): ButtonConfiguration {
        return CostrictProjectViewButtonConfiguration()
    }
    
    /**
     * Costrict project view button configuration.
     */
    private class CostrictProjectViewButtonConfiguration : ButtonConfiguration {
        override fun isButtonVisible(buttonType: ButtonType): Boolean {
            // Only show specific buttons for project view
            return true
        }
        
        override fun getVisibleButtons(): List<ButtonType> {
            return emptyList()
        }
    }

    /**
     * Action to perform code review on selected files.
     * Triggers code review command with the selected file paths.
     * Note: Directories are filtered out and not supported.
     */
    class CodeReviewAction : AnAction("Code Review") {
        private val logger: Logger = Logger.getInstance(CodeReviewAction::class.java)
        
        init {
            templatePresentation.icon = AllIcons.Actions.Preview
            templatePresentation.text = "Code Review"
            templatePresentation.description = "Review code for selected files (directories not supported)"
        }
        
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            
            // Try to get files from multiple sources
            var virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.filter { !it.isDirectory }?.toList()
            if (virtualFiles != null && virtualFiles.isNotEmpty()) {
                logger.info("Using VIRTUAL_FILE_ARRAY: ${virtualFiles.size} file(s) (directories filtered out)")
            } else {
                virtualFiles = null
            }
            
            // If VIRTUAL_FILE_ARRAY is null or empty, try single file
            if (virtualFiles == null) {
                val singleFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
                if (singleFile != null && !singleFile.isDirectory) {
                    virtualFiles = listOf(singleFile)
                    logger.info("Using single VIRTUAL_FILE: ${singleFile.name}")
                } else if (singleFile != null && singleFile.isDirectory) {
                    logger.info("Skipping single VIRTUAL_FILE because it's a directory: ${singleFile.name}")
                }
            }
            
            // If still null, try to extract from SELECTED_ITEMS (tree nodes)
            if (virtualFiles == null) {
                val selectedItems = e.getData(PlatformDataKeys.SELECTED_ITEMS)
                virtualFiles = selectedItems?.mapNotNull { item ->
                    val file = when (item) {
                        is VirtualFile -> item
                        is PsiFileNode -> item.virtualFile
                        is PsiDirectoryNode -> item.virtualFile
                        is AbstractTreeNode<*> -> {
                            val value = item.value
                            when (value) {
                                is VirtualFile -> value
                                is PsiElement -> value.containingFile?.virtualFile
                                else -> null
                            }
                        }
                        else -> null
                    }
                    
                    // Filter out directories - only allow files
                    if (file != null && file.isDirectory) {
                        logger.info("Skipping directory in action: ${file.name}")
                        null
                    } else {
                        file
                    }
                }
                
                if (virtualFiles != null) {
                    logger.info("Extracted ${virtualFiles.size} file(s) from SELECTED_ITEMS (directories filtered out)")
                }
            }
            
            // Validate we have files
            if (virtualFiles == null || virtualFiles.isEmpty()) {
                logger.warn("No files selected for code review")
                return
            }
            
            // Collect file paths
            val filePaths = virtualFiles.map { it.path }
            
            logger.info("🔍 Triggering code review for ${filePaths.size} file(s): ${filePaths.joinToString(", ")}")
            
            // Prepare arguments for the command
            val args = mutableMapOf<String, Any?>()
            args["filePaths"] = filePaths
            args["fileCount"] = filePaths.size
            
            // Execute code review command
            executeCommand("zgsm.reviewFilesAndFoldersJetbrains", project, args)
        }
        
        override fun update(e: AnActionEvent) {
            // Try multiple data keys to get selected files
            val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            val singleFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            val selectedItems = e.getData(PlatformDataKeys.SELECTED_ITEMS)
            val project = e.getData(CommonDataKeys.PROJECT)
            val place = e.place
            
            // Info logging to track button state and context
            logger.info("=== CodeReview Button Update ===")
            logger.info("Action place: $place")
            logger.info("Project: ${project?.name}")
            logger.info("VIRTUAL_FILE_ARRAY: ${selectedFiles?.size ?: "null"}")
            logger.info("VIRTUAL_FILE (single): ${singleFile?.name ?: "null"}")
            logger.info("SELECTED_ITEMS: ${selectedItems?.size ?: "null"}")
            
            // Try to extract VirtualFiles from selected items (including tree nodes)
            // Only allow files, not directories
            val filesFromItems = selectedItems?.mapNotNull { item ->
                logger.info("Processing item type: ${item?.javaClass?.name}")
                
                val virtualFile = when (item) {
                    is VirtualFile -> {
                        logger.info("  → Direct VirtualFile: ${item.name}, isDirectory: ${item.isDirectory}")
                        item
                    }
                    is PsiFileNode -> {
                        val file = item.virtualFile
                        logger.info("  → PsiFileNode, extracted file: ${file?.name}, isDirectory: ${file?.isDirectory}")
                        file
                    }
                    is PsiDirectoryNode -> {
                        val file = item.virtualFile
                        logger.info("  → PsiDirectoryNode (directory), extracted file: ${file?.name}, isDirectory: ${file?.isDirectory}")
                        file
                    }
                    is AbstractTreeNode<*> -> {
                        // Generic tree node - try to get VirtualFile from value
                        val value = item.value
                        logger.info("  → AbstractTreeNode, value type: ${value?.javaClass?.name}")
                        
                        val file = when (value) {
                            is VirtualFile -> value
                            is PsiElement -> value.containingFile?.virtualFile
                            else -> null
                        }
                        logger.info("  → Extracted file: ${file?.name}, isDirectory: ${file?.isDirectory}")
                        file
                    }
                    else -> {
                        logger.info("  → Unknown type, cannot extract file")
                        null
                    }
                }
                
                // Filter out directories - only allow files
                if (virtualFile != null && virtualFile.isDirectory) {
                    logger.info("  ❌ Skipping directory: ${virtualFile.name}")
                    null
                } else if (virtualFile != null) {
                    logger.info("  ✅ Accepted file: ${virtualFile.name}")
                    virtualFile
                } else {
                    null
                }
            }
            
            // Determine which source has files (exclude directories)
            val hasFiles = when {
                selectedFiles != null && selectedFiles.isNotEmpty() -> {
                    // Filter out directories
                    val actualFiles = selectedFiles.filter { !it.isDirectory }
                    if (actualFiles.isNotEmpty()) {
                        logger.info("✅ Using VIRTUAL_FILE_ARRAY: ${actualFiles.size} file(s) (${selectedFiles.size - actualFiles.size} directories filtered)")
                        true
                    } else {
                        logger.info("❌ VIRTUAL_FILE_ARRAY only contains directories, no files")
                        false
                    }
                }
                singleFile != null -> {
                    if (!singleFile.isDirectory) {
                        logger.info("✅ Using VIRTUAL_FILE (single): ${singleFile.name}")
                        true
                    } else {
                        logger.info("❌ VIRTUAL_FILE is a directory: ${singleFile.name}")
                        false
                    }
                }
                filesFromItems != null && filesFromItems.isNotEmpty() -> {
                    logger.info("✅ Using SELECTED_ITEMS: ${filesFromItems.size} file(s) - ${filesFromItems.joinToString(", ") { it.name }}")
                    true
                }
                else -> {
                    logger.info("❌ No files found in any data key")
                    false
                }
            }
            
            logger.info("CodeReview button isEnabled: $hasFiles, presentation.isVisible: ${e.presentation.isVisible}")
            
            e.presentation.isEnabled = hasFiles
        }
    }
}

