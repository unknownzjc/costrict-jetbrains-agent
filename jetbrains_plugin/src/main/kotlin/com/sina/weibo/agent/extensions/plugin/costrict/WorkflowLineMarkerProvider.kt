// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.openapi.actionSystem.AnAction
import com.sina.weibo.agent.actions.WorkflowRunCommandAction
import com.sina.weibo.agent.actions.WorkflowDebugRunCommandAction
import com.sina.weibo.agent.actions.WorkflowEditConfigAction

/**
 * Costrict extension provider implementation
 */
class WorkflowLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        val text = element.text ?: return null

        // 识别简单的npm命令
        if (text.startsWith("npm install") || text.startsWith("npm run")) {
            val runAction = WorkflowRunCommandAction(text)
            val debugAction = WorkflowDebugRunCommandAction(text)
            val editAction = WorkflowEditConfigAction(text)

            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                { "运行 '$text'" },
                runAction, debugAction, editAction // 多个动作 → 出现下拉菜单
            )
        } else if (text.startsWith("- [ ]") || text.startsWith("- [x]") || text.startsWith("- [-]")) {
            val runAction = WorkflowRunCommandAction("run")
            val debugAction = WorkflowDebugRunCommandAction("retry")
            val editAction = WorkflowEditConfigAction("test")

            return Info(
                AllIcons.RunConfigurations.TestState.Run,
                { "运行" },
                // { "运行 '$text'" },
                runAction, debugAction, editAction // 多个动作 → 出现下拉菜单
            )
        }

        

        return null
    }
}