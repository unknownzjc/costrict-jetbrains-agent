// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.extensions.plugin.costrict

object CostrictFileConstants {
    const val COSPEC_DIR = "/.cospec/"
    
    // 支持的文件
    const val REQUIREMENTS_FILE = "requirements.md"
    const val DESIGN_FILE = "design.md"
    const val TASKS_FILE = "tasks.md"
    
    val SUPPORTED_FILES = setOf(REQUIREMENTS_FILE, DESIGN_FILE, TASKS_FILE)
    
    // 任务状态标记
    const val TASK_PENDING = "- [ ]"
    const val TASK_IN_PROGRESS = "- [-]"
    const val TASK_COMPLETED = "- [x]"
    
    // 工具提示文本
    const val TOOLTIP_SYNC_TO_DESIGN = "同步变更至设计"
    const val TOOLTIP_SYNC_TO_TASKS = "同步变更至任务"
    
    // 任务状态文本
    const val STATUS_PENDING_TEXT = "待执行任务"
    const val STATUS_IN_PROGRESS_TEXT = "进行中任务"
    const val STATUS_COMPLETED_TEXT = "已完成任务"
}