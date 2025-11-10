// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 同步需求到设计的操作类
 */
class SyncToDesignAction(private val command: String = "同步需求到设计") : AnAction(command) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        Messages.showInfoMessage(project, "正在同步需求变更至设计文档", "同步中")
        // 这里添加实际同步需求到设计的逻辑
    }
}