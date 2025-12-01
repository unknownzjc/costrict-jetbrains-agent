// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.diff

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.commands.ICommand

/**
 * 处理 VSCode "vscode.changes" 命令的命令类
 * 
 * @property project 当前项目上下文
 * @property handler 差异视图处理器实例
 */
class DiffViewCommand(
    private val project: Project,
    private val handler: DiffViewHandler
) : ICommand {
    
    override fun getId(): String = "vscode.changes"
    
    override fun getMethod(): String = "handleChangesCommand"
    
    override fun handler(): Any = handler
    
    override fun returns(): String? = null
}