// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actors

import com.sina.weibo.agent.model.WorkspaceData

/**
 * Main thread workspace service interface
 * Provides workspace-related operations for the main thread
 */
interface MainThreadWorkspaceShape {
    /**
     * Get current workspace data
     * @return Workspace data or null if no workspace is open
     */
    fun getCurrentWorkspaceData(): WorkspaceData?
    
    /**
     * Get workspace data for a specific project
     * @param projectId The project identifier
     * @return Workspace data or null if project not found
     */
    fun getProjectWorkspaceData(projectId: String): WorkspaceData?
    
    /**
     * Get all available workspace data
     * @return List of all workspace data
     */
    fun getAllWorkspaceData(): List<WorkspaceData>
    
    /**
     * Check if a workspace is currently open
     * @return true if a workspace is open, false otherwise
     */
    fun isWorkspaceOpen(): Boolean
    
    /**
     * Get workspace folders for current workspace
     * @return List of workspace folders or empty list if no workspace is open
     */
    fun getWorkspaceFolders(): List<WorkspaceData>
    
    /**
     * Start file search in the workspace
     * @param uri The URI information containing path, scheme, and other metadata
     * @param options Search configuration options including ignore settings and patterns
     * @return Search handle or identifier
     */
    fun startFileSearch(uri: Map<String, Any>, options: Map<String, Any>): Any?
}
