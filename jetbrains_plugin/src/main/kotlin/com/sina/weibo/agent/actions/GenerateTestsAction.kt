// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 生成测试用例的操作类
 */
class GenerateTestsAction(private val command: String = "生成测试用例") : AnAction(command) {
    override fun actionPerformed(e: AnActionEvent) {
        println("GenerateTestsAction: 开始执行 actionPerformed")
        val project: Project = e.project ?: run {
            println("GenerateTestsAction: 项目为空，返回")
            return
        }
        println("GenerateTestsAction: 显示信息对话框")
        Messages.showInfoMessage(project, "正在生成测试用例", "生成中")
        // 这里添加实际生成测试用例的逻辑
        println("GenerateTestsAction: 测试用例生成完成")
    }
}