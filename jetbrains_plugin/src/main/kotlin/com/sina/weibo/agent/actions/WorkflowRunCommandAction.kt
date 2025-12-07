package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class WorkflowRunCommandAction(private val command: String) : AnAction("Run '$command'") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        Messages.showInfoMessage(project, "执行命令：$command", "运行中")
        // 这里你可以换成实际执行 shell 命令的逻辑
    }
}
