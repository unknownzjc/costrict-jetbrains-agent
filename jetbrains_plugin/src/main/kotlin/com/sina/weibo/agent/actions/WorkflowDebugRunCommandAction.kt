package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class WorkflowDebugRunCommandAction(private val command: String) : AnAction("Debug '$command'") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        Messages.showInfoMessage(project, "调试命令：$command", "调试模式")
    }
}
