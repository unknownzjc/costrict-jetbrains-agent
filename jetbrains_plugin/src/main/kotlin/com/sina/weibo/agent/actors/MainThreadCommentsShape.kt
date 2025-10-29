// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actors

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sina.weibo.agent.comments.CommentManager

interface MainThreadCommentsShape : Disposable {
    fun registerCommentController(handle: Int, id: String, label: String, extensionId: String)
    fun unregisterCommentController(handle: Int)
    fun updateCommentControllerFeatures(handle: Int, features: Map<String, Any?>)
    fun updateCommentingRanges(handle: Int, resourceHints: Map<String, Any?>?)

    fun createCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        threadId: String,
        resource: Map<String, Any?>,
        range: Map<String, Any?>?,
        comments: List<Map<String, Any?>>,
        extensionId: String,
        isTemplate: Boolean,
        editorId: String? = null
    ): Map<String, Any?>?

    fun updateCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        threadId: String,
        resource: Map<String, Any?>,
        changes: Map<String, Any?>
    )

    fun deleteCommentThread(handle: Int, commentThreadHandle: Int)
    suspend fun revealCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        commentUniqueId: Int,
        options: Map<String, Any?>
    )

    fun hideCommentThread(handle: Int, commentThreadHandle: Int)
}

class MainThreadComments(project: Project) : MainThreadCommentsShape {
    private val logger = Logger.getInstance(MainThreadComments::class.java)
    private val commentManager = project.getService(CommentManager::class.java)

    override fun registerCommentController(handle: Int, id: String, label: String, extensionId: String) {
        try {
            commentManager.registerController(handle, id, label, extensionId)
        } catch (e: Exception) {
            logger.error("Failed to register comment controller: $id", e)
        }
    }

    override fun unregisterCommentController(handle: Int) {
        try {
            commentManager.unregisterController(handle)
        } catch (e: Exception) {
            logger.error("Failed to unregister comment controller: $handle", e)
        }
    }

    override fun updateCommentControllerFeatures(handle: Int, features: Map<String, Any?>) {
        try {
            commentManager.updateControllerFeatures(handle, features)
        } catch (e: Exception) {
            logger.error("Failed to update comment controller features for $handle", e)
        }
    }

    override fun updateCommentingRanges(handle: Int, resourceHints: Map<String, Any?>?) {
        try {
            commentManager.updateCommentingRanges(handle, resourceHints)
        } catch (e: Exception) {
            logger.error("Failed to update commenting ranges for $handle", e)
        }
    }

    override fun createCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        threadId: String,
        resource: Map<String, Any?>,
        range: Map<String, Any?>?,
        comments: List<Map<String, Any?>>,
        extensionId: String,
        isTemplate: Boolean,
        editorId: String?
    ): Map<String, Any?>? {
        return try {
            commentManager.createThread(
                handle,
                commentThreadHandle,
                threadId,
                resource,
                range,
                comments,
                extensionId,
                isTemplate,
                editorId
            )
        } catch (e: Exception) {
            logger.error("Failed to create comment thread: $commentThreadHandle", e)
            null
        }
    }

    override fun updateCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        threadId: String,
        resource: Map<String, Any?>,
        changes: Map<String, Any?>
    ) {
        try {
            commentManager.updateThread(handle, commentThreadHandle, threadId, resource, changes)
        } catch (e: Exception) {
            logger.error("Failed to update comment thread: $commentThreadHandle", e)
        }
    }

    override fun deleteCommentThread(handle: Int, commentThreadHandle: Int) {
        try {
            commentManager.deleteThread(handle, commentThreadHandle)
        } catch (e: Exception) {
            logger.error("Failed to delete comment thread: $commentThreadHandle", e)
        }
    }

    override suspend fun revealCommentThread(
        handle: Int,
        commentThreadHandle: Int,
        commentUniqueId: Int,
        options: Map<String, Any?>
    ) {
        try {
            commentManager.revealThread(handle, commentThreadHandle, commentUniqueId, options)
        } catch (e: Exception) {
            logger.error("Failed to reveal comment thread: $commentThreadHandle", e)
        }
    }

    override fun hideCommentThread(handle: Int, commentThreadHandle: Int) {
        try {
            commentManager.hideThread(handle, commentThreadHandle)
        } catch (e: Exception) {
            logger.error("Failed to hide comment thread: $commentThreadHandle", e)
        }
    }

    override fun dispose() {
        logger.info("Disposing MainThreadComments")
    }
}
