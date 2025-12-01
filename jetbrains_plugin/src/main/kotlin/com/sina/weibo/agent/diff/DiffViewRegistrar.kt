// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.diff

import com.intellij.openapi.project.Project
import com.sina.weibo.agent.commands.ICommandRegistry

/**
 * 注册差异视图相关命令的工具类
 */
object DiffViewRegistrar {
    
    /**
     * 注册所有差异视图相关的命令到命令注册表
     * 
     * @param project 当前项目上下文
     * @param registry 命令注册表实例
     */
    fun registerDiffCommands(project: Project, registry: ICommandRegistry) {
        val diffHandler = DiffViewHandler(project)
        val diffCommand = DiffViewCommand(project, diffHandler)
        
        registry.registerCommand(diffCommand)
    }
}