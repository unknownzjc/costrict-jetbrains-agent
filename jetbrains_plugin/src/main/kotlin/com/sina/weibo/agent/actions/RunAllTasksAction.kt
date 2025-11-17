// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 运行所有任务的操作类
 */
class RunAllTasksAction(private val command: String = "运行所有任务") : AnAction(command) {
    override fun actionPerformed(e: AnActionEvent) {
        println("RunAllTasksAction: 开始执行 actionPerformed")
        val project: Project = e.project ?: run {
            println("RunAllTasksAction: 项目为空，返回")
            return
        }
        println("RunAllTasksAction: 显示信息对话框")
        Messages.showInfoMessage(project, "正在执行所有任务", "运行中")
        // 这里添加实际运行所有任务的逻辑
        println("RunAllTasksAction: 所有任务执行完成")
    }
}