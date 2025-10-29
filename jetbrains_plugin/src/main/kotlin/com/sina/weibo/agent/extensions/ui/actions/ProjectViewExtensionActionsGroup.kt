// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.ui.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbAware
import com.sina.weibo.agent.extensions.core.ExtensionManager
import com.sina.weibo.agent.extensions.config.ExtensionProvider
import com.sina.weibo.agent.extensions.common.ExtensionChangeListener
import com.sina.weibo.agent.extensions.plugin.costrict.CostrictProjectViewButtonProvider
import com.sina.weibo.agent.extensions.ui.buttons.ExtensionButtonProvider

/**
 * Project view extension actions group that shows buttons for file/directory context menus.
 * This class is specifically designed for project view right-click menu operations.
 * Currently only supports Costrict extension.
 */
class ProjectViewExtensionActionsGroup : DefaultActionGroup(), DumbAware, ActionUpdateThreadAware, ExtensionChangeListener {
    
    private val logger = Logger.getInstance(ProjectViewExtensionActionsGroup::class.java)
    
    /**
     * Cache for button provider, extension ID and actions.
     * These are updated when the extension changes.
     */
    private var cachedButtonProvider: ExtensionButtonProvider? = null
    private var cachedExtensionId: String? = null
    private var cachedActions: List<AnAction>? = null
    
    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        if (project == null) {
            e.presentation.isVisible = false
            return
        }
        
        try {
            val extensionManager = ExtensionManager.getInstance(project)
            val currentProvider = extensionManager.getCurrentProvider()
            
            if (currentProvider != null) {
                val extensionId = currentProvider.getExtensionId()
                
                // Only support Costrict extension for project view
                if (extensionId == "costrict") {
                    // Check if cache needs to be updated
                    if (cachedExtensionId != extensionId || cachedActions == null) {
                        updateCachedActions(currentProvider, project)
                    }
                    
                    // Use cached actions
                    if (cachedActions != null) {
                        removeAll()
                        cachedActions!!.forEach { action ->
                            add(action)
                        }
                        e.presentation.isVisible = true
                        logger.debug("Using cached project view actions for extension: $extensionId")
                    }
                } else {
                    // Hide menu for other extensions
                    e.presentation.isVisible = false
                    logger.debug("Extension $extensionId is not supported in project view, hiding menu")
                }
            } else {
                e.presentation.isVisible = false
                logger.debug("No current extension provider, hiding project view actions")
            }
        } catch (exception: Exception) {
            logger.warn("Failed to load project view actions", exception)
            e.presentation.isVisible = false
        }
    }
    
    private fun updateCachedActions(provider: ExtensionProvider, project: Project) {
        val extensionId = provider.getExtensionId()
        
        // Create button provider instance for the extension
        val buttonProvider = when (extensionId) {
            "costrict" -> CostrictProjectViewButtonProvider()
            else -> null
        }
        
        if (buttonProvider != null) {
            // Create and cache actions
            val actions = buttonProvider.getButtons(project)
            
            // Update cache
            cachedButtonProvider = buttonProvider
            cachedExtensionId = extensionId
            cachedActions = actions
            
            logger.debug("Updated cached project view actions for extension: $extensionId, count: ${actions.size}")
        }
    }

    /**
     * Specifies which thread should be used for updating this action.
     * Returns BGT to ensure updates happen on the background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    /**
     * Called when the current extension changes.
     * This method is part of the ExtensionChangeListener interface.
     * 
     * @param newExtensionId The ID of the new extension
     */
    override fun onExtensionChanged(newExtensionId: String) {
        logger.info("Extension changed to: $newExtensionId, refreshing project view actions")
        
        // Clear cache when extension changes
        cachedButtonProvider = null
        cachedExtensionId = null
        cachedActions = null
        
        // Note: The action group will be automatically refreshed when the UI is next displayed
        // No need to manually trigger an update here
    }
}

