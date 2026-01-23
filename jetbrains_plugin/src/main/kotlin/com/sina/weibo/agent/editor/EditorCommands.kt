// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.editor

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.contents.DiffContent
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.WindowManager
import com.sina.weibo.agent.commands.CommandRegistry
import com.sina.weibo.agent.commands.ICommand
import com.sina.weibo.agent.util.URI
import com.sina.weibo.agent.util.URIComponents
import com.sina.weibo.agent.util.NotificationUtil
import java.io.File
import java.nio.file.Paths

/**
 * Registers commands related to editor API operations
 * Currently registers the workbench diff command for file comparison
 *
 * @param project The current IntelliJ project
 * @param registry The command registry to register commands with
 */
fun registerOpenEditorAPICommands(project: Project,registry: CommandRegistry) {

    registry.registerCommand(
        object : ICommand{
            override fun getId(): String {
                return "_workbench.diff"
            }
            override fun getMethod(): String {
                return "workbench_diff"
            }

            override fun handler(): Any {
                return OpenEditorAPICommands(project)
            }

            override fun returns(): String? {
                return "void"
            }

        }
    )

    registry.registerCommand(
        object : ICommand{
            override fun getId(): String {
                return "vscode.openFolder"
            }
            override fun getMethod(): String {
                return "openFolder"
            }

            override fun handler(): Any {
                return OpenEditorAPICommands(project)
            }

            override fun returns(): String? {
                return "void"
            }

        }
    )
}

/**
 * Handles editor API commands for operations like opening diff editors
 */
class OpenEditorAPICommands(val project: Project) {
    private val logger = Logger.getInstance(OpenEditorAPICommands::class.java)
    
    /**
     * Opens a diff editor to compare two files
     *
     * @param left Map containing URI components for the left file
     * @param right Map containing URI components for the right file
     * @param title Optional title for the diff editor
     * @param columnOrOptions Optional column or options for the diff editor
     * @return null after operation completes
     */
    suspend fun workbench_diff(left: Map<String, Any?>, right : Map<String, Any?>, title : String?,columnOrOptions : Any?): Any?{
        val rightURI = createURI(right)
        val leftURI = createURI(left)
        logger.info("Opening diff: ${rightURI.path}")
        val content1 = createContent(left,project)
        val content2 = createContent(right,project)
        if (content1 != null && content2 != null){
            project.getService(EditorAndDocManager::class.java).openDiffEditor(leftURI,rightURI,title?:"File Comparison")
        }
        logger.info("Opening diff completed: ${rightURI.path}")
        return null;
    }

    /**
     * Opens a folder as a new project in IntelliJ IDEA
     *
     * @param uri Map containing URI components for the folder to open
     * @param options Optional map containing options such as forceNewWindow
     * @return null after operation completes
     */
    suspend fun openFolder(uri: Map<String, Any?>, options: Map<String, Any?>?): Any? {
        val folderURI = createURI(uri)
        logger.info("Opening folder: ${folderURI.path}")
        
        val folderPath = folderURI.path
        val folderFile = File(folderPath)
        
        // Check if folder exists and is a directory
        if (!folderFile.exists() || !folderFile.isDirectory) {
            logger.warn("Folder does not exist or is not a directory: $folderPath")
            NotificationUtil.showError("Failed to Open Project", "Folder does not exist or is not a valid directory: $folderPath", project)
            return null
        }
        
        // Check if we need to force opening in a new window
        val forceNewWindow = options?.get("forceNewWindow") as? Boolean ?: false
        
        // Check if the folder is already opened as a project
        val projectManager = ProjectManager.getInstance()
        val openProjects = projectManager.openProjects
        val existingProject = openProjects.find { it.basePath == folderPath }
        
        if (existingProject != null && !forceNewWindow) {
            // Project is already open, bring it to front
            logger.info("Project already open, bringing to front: $folderPath")
            ApplicationManager.getApplication().invokeLater {
                val frame = WindowManager.getInstance().getFrame(existingProject)
                frame?.toFront()
                frame?.requestFocus()
            }
            return null
        }
        
        // Open the project in a new window or current window
        ApplicationManager.getApplication().invokeLater {
            try {
                val path = Paths.get(folderPath)
                logger.info("Opening project: $folderPath, forceNewWindow: $forceNewWindow")
                ProjectUtil.openOrImport(path, null, forceNewWindow)
                logger.info("Project opened successfully: $folderPath")
            } catch (e: Exception) {
                logger.error("Failed to open project: $folderPath", e)
                NotificationUtil.showError("Failed to Open Project", e.message ?: "Unknown error", null)
            }
        }
        
        return null
    }

    /**
     * Creates a DiffContent object from URI components
     *
     * @param uri Map containing URI components
     * @param project The current IntelliJ project
     * @return DiffContent object or null if creation fails
     */
    fun createContent(uri: Map<String, Any?>, project: Project) : DiffContent?{
        val path = uri["path"]
        val scheme = uri["scheme"]
        val query = uri["query"]
        val fragment = uri["fragment"]
        if(scheme != null){
            val contentFactory = DiffContentFactory.getInstance()
            if(scheme == "file"){
                val vfs = LocalFileSystem.getInstance()
                val fileIO = File(path as String)
                if(!fileIO.exists()){
                    fileIO.createNewFile()
                    vfs.refreshIoFiles(listOf(fileIO.parentFile))
                }

                val file = vfs.refreshAndFindFileByPath(path as String) ?: run {
                    logger.warn("File not found: $path")
                    return null
                }
                return contentFactory.create(project, file)
            }else if(scheme == "costrict-diff"){
                val string = if(query != null){
                    val bytes = java.util.Base64.getDecoder().decode(query as String)
                    String(bytes)
                }else ""
                val content = contentFactory.create(project, string)
                return content
            }
            return null
        }else{
            return null
        }
    }
}

/**
 * Creates a URI object from a map of URI components
 *
 * @param map Map containing URI components (scheme, authority, path, query, fragment)
 * @return URI object constructed from the components
 */
fun createURI(map: Map<String, Any?>): URI {
    val authority = if (map["authority"] != null) map["authority"] as String else ""
    val query = if (map["query"] != null) map["query"] as String else ""
    val fragment = if (map["fragment"] != null) map["fragment"] as String else ""

    val uriComponents = object : URIComponents {
        override val scheme: String = map["scheme"] as String
        override val authority: String = authority
        override val path: String = map["path"] as String
        override val query: String = query
        override val fragment: String = fragment
    }
    return URI.from(uriComponents)
}