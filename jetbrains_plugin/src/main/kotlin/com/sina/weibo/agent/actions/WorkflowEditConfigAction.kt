package com.sina.weibo.agent.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class WorkflowEditConfigAction(private val command: String) : AnAction("Edit '$command'") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        Messages.showInfoMessage(project, "打开配置编辑器（占位）", "配置")
    }
}
