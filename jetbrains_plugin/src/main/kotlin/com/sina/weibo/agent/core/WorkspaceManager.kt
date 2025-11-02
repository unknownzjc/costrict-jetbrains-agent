// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.util.io.FileUtil
import com.sina.weibo.agent.actors.MainThreadWorkspaceShape
import com.sina.weibo.agent.model.StaticWorkspaceData
import com.sina.weibo.agent.model.WorkspaceData
import com.sina.weibo.agent.model.WorkspaceFolder
import com.sina.weibo.agent.util.URI
import java.io.File
import java.util.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Workspace Manager
 * Responsible for retrieving and managing IDEA workspace information.
 * Provides functionality to access project workspace data and folders.
 */
@Service(Service.Level.PROJECT)
class WorkspaceManager(val project: Project) : MainThreadWorkspaceShape {
    private val logger = Logger.getInstance(WorkspaceManager::class.java)

    /**
     * Gets the current workspace data.
     * @return Workspace data or null (if no project is open)
     */
    override fun getCurrentWorkspaceData(): WorkspaceData? {
        return getProjectWorkspaceData(project)
    }

    /**
     * Gets workspace data for a specific project.
     *
     * @param project The project to get workspace data for
     * @return Workspace data or null if the project is null
     */
    fun getProjectWorkspaceData(project: Project): WorkspaceData? {

        // Create workspace ID (using hash value of the project's base path)
        val workspaceId = getWorkspaceId(project)
        val workspaceName = project.name

        // Create static workspace data
        val staticWorkspaceData = StaticWorkspaceData(
            id = workspaceId,
            name = workspaceName,
            transient = false,
            // Configuration can be the project's .idea directory or project configuration file
            configuration = project.basePath?.let { URI.file("$it/.idea") },
            isUntitled = false
        )

        // Get workspace folders
        val workspaceFolders = getWorkspaceFolders(project)

        return WorkspaceData(staticWorkspaceData, workspaceFolders)
    }
    
    /**
     * Gets the workspace ID for a project.
     *
     * @param project The project
     * @return The workspace ID as a string
     */
    private fun getWorkspaceId(project: Project): String {
        // Use the hash value of the project path as ID
        val basePath = project.basePath ?: return UUID.randomUUID().toString()
        return basePath.hashCode().toString()
    }
    
    /**
     * Gets workspace folders for a project.
     *
     * @param project The project
     * @return List of workspace folders
     */
    private fun getWorkspaceFolders(project: Project): List<WorkspaceFolder> {
        val folders = mutableListOf<WorkspaceFolder>()
        val basePath = project.basePath ?: return folders
        
        // Add project root directory as the main workspace folder
        folders.add(WorkspaceFolder(
            uri = URI.file(basePath),
            name = project.name,
            index = 0
        ))
        
        // Get the virtual file for the project root directory - wrapped in ReadAction
        val projectDir = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
            LocalFileSystem.getInstance().findFileByPath(basePath)
        }
        if (projectDir == null || !projectDir.isDirectory) {
            return folders
        }
        
//        // Get subdirectories
//        val contentRoots = projectDir.children
//
//        // Filter files to get subfolders
//        val subFolders = contentRoots.filter { file: VirtualFile ->
//            file.isDirectory &&
//            !file.name.startsWith(".") &&
//            file.name !in listOf("out", "build", "target", "node_modules", ".idea", "dist", "bin", "obj")
//        }
//
//        // Add subfolders
//        subFolders.forEachIndexed { index, folder ->
//            if (folder.path != basePath) {
//                folders.add(WorkspaceFolder(
//                    uri = URI.file(folder.path),
//                    name = folder.name,
//                    index = index + 1  // Start from 1, because 0 is the project root directory
//                ))
//            }
//        }
        
        return folders
    }

    /**
     * Get workspace data for a specific project by ID
     * @param projectId The project identifier
     * @return Workspace data or null if project not found
     */
    override fun getProjectWorkspaceData(projectId: String): WorkspaceData? {
        // Find project by ID - this is a simplified implementation
        // In a real scenario, you would need to map project IDs to actual Project instances
        val allProjects = ProjectManager.getInstance().openProjects
        val targetProject = allProjects.find { getWorkspaceId(it) == projectId }
        return targetProject?.let { getProjectWorkspaceData(it) }
    }

    /**
     * Get all available workspace data
     * @return List of all workspace data
     */
    override fun getAllWorkspaceData(): List<WorkspaceData> {
        val allProjects = ProjectManager.getInstance().openProjects
        return allProjects.mapNotNull { getProjectWorkspaceData(it) }
    }

    /**
     * Check if a workspace is currently open
     * @return true if a workspace is open, false otherwise
     */
    override fun isWorkspaceOpen(): Boolean {
        return project.isOpen && !project.isDisposed
    }

    /**
     * Get workspace folders for current workspace
     * @return List of workspace folders or empty list if no workspace is open
     */
    override fun getWorkspaceFolders(): List<WorkspaceData> {
        val currentWorkspace = getCurrentWorkspaceData()
        return if (currentWorkspace != null) listOf(currentWorkspace) else emptyList()
    }

    /**
     * Start file search in the workspace
     * @param uri The URI information containing path, scheme, and other metadata
     * @param options Search configuration options including ignore settings and patterns
     * @return Search handle or identifier
     */
    override fun startFileSearch(uri: Map<String, Any>, options: Map<String, Any>): Any? {
        return try {
            logger.info("RPC startFileSearch called with uri: $uri, options: $options")
            
            // Extract path from URI
            val fsPath = uri["fsPath"] as? String ?: uri["path"] as? String
            if (fsPath.isNullOrBlank()) {
                logger.warn("No valid path found in URI: $uri")
                return null
            }

            // Extract search options
            val includePattern = options["includePattern"] as? String ?: "**/{requirements,design,tasks}.md"
            // Note: These options are extracted but not currently used in the implementation
            // They may be used for future enhancements
            @Suppress("UNUSED_VARIABLE")
            val disregardIgnoreFiles = options["disregardIgnoreFiles"] as? Boolean ?: false
            @Suppress("UNUSED_VARIABLE")
            val disregardExcludeSettings = options["disregardExcludeSettings"] as? Boolean ?: false
            @Suppress("UNUSED_VARIABLE")
            val disregardSearchExcludeSettings = options["disregardSearchExcludeSettings"] as? Boolean ?: false

            logger.info("Starting file search in path: $fsPath with pattern: $includePattern")

            // Convert to Path object
            val searchPath = Paths.get(fsPath)
            if (!Files.exists(searchPath) || !Files.isDirectory(searchPath)) {
                logger.warn("Search path does not exist or is not a directory: $fsPath")
                return emptyList<String>()
            }

            // Perform file search
            val matchedFiles = mutableListOf<String>()
            
            // Search for files matching the pattern
            val targetFilenames = setOf("requirements.md", "design.md", "tasks.md")
            
            Files.walk(searchPath).use { paths ->
                paths.filter { path ->
                    path.isRegularFile() && targetFilenames.contains(path.name.lowercase())
                }.forEach { matchedFile ->
                    matchedFiles.add(matchedFile.toString())
                    logger.info("Found matching file: ${matchedFile}")
                }
            }

            logger.info("File search completed. Found ${matchedFiles.size} files.")
            matchedFiles
            
        } catch (e: Exception) {
            logger.error("Error during file search in WorkspaceManager.startFileSearch", e)
            null
        }
    }
}
