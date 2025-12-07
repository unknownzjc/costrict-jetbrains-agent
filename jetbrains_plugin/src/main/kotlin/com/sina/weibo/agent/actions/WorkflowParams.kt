// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Workflow Action 参数数据类
 * 基于 RPC 规范定义的参数结构
 */
data class WorkflowActionParams(
    // 文件信息
    val filePath: String,
    val documentType: String, // "requirements" | "design" | "tasks"
    
    // 位置信息
    val lineNumber: Int, // 0-based
    val startLine: Int? = null,
    val endLine: Int? = null,
    
    // 内容信息
    val selectedText: String? = null,
    val taskText: String? = null,
    val allTasksContent: String? = null,
    val requirementsContent: String? = null,
    val designContent: String? = null,
    
    // 任务状态
    val taskStatus: String? = null, // "pending" | "in_progress" | "completed"
    val isFirstTask: Boolean? = null,
    
    // 操作类型
    val actionType: String, // "run" | "run_all" | "retry" | "update" | "run_test" | "sync_to_design" | "sync_to_tasks"
    
    // 额外信息
    val scopePath: String? = null,
    val diffContent: String? = null
) {
    companion object {
        private val gson = Gson()
        
        /**
         * 验证参数是否有效
         */
        fun validate(params: WorkflowActionParams): ValidationResult {
            val errors = mutableListOf<String>()
            
            // 验证必需字段
            if (params.filePath.isBlank()) {
                errors.add("filePath cannot be empty")
            }
            
            if (params.lineNumber < 0) {
                errors.add("lineNumber must be non-negative")
            }
            
            // 验证 documentType
            val validDocumentTypes = setOf("requirements", "design", "tasks")
            if (params.documentType !in validDocumentTypes) {
                errors.add("documentType must be one of: $validDocumentTypes")
            }
            
            // 验证 actionType
            val validActionTypes = setOf("run", "run_all", "retry", "update", "run_test", "sync_to_design", "sync_to_tasks")
            if (params.actionType !in validActionTypes) {
                errors.add("actionType must be one of: $validActionTypes")
            }
            
            // 根据 actionType 验证特定字段
            when (params.actionType) {
                "run", "retry" -> {
                    if (params.taskText.isNullOrBlank()) {
                        errors.add("taskText is required for actionType '${params.actionType}'")
                    }
                    if (params.documentType != "tasks") {
                        errors.add("documentType must be 'tasks' for actionType '${params.actionType}'")
                    }
                }
                "run_all" -> {
                    if (params.allTasksContent.isNullOrBlank()) {
                        errors.add("allTasksContent is required for actionType 'run_all'")
                    }
                    if (params.documentType != "tasks") {
                        errors.add("documentType must be 'tasks' for actionType 'run_all'")
                    }
                }
                "update" -> {
                    if (params.selectedText.isNullOrBlank()) {
                        errors.add("selectedText is required for actionType 'update'")
                    }
                    if (params.documentType !in setOf("requirements", "design")) {
                        errors.add("documentType must be 'requirements' or 'design' for actionType 'update'")
                    }
                }
                "run_test" -> {
                    if (params.scopePath.isNullOrBlank()) {
                        errors.add("scopePath is required for actionType 'run_test'")
                    }
                    if (params.documentType != "tasks") {
                        errors.add("documentType must be 'tasks' for actionType 'run_test'")
                    }
                }
                "sync_to_design" -> {
                    if (params.requirementsContent.isNullOrBlank()) {
                        errors.add("requirementsContent is required for actionType 'sync_to_design'")
                    }
                    if (params.documentType != "requirements") {
                        errors.add("documentType must be 'requirements' for actionType 'sync_to_design'")
                    }
                }
                "sync_to_tasks" -> {
                    if (params.designContent.isNullOrBlank()) {
                        errors.add("designContent is required for actionType 'sync_to_tasks'")
                    }
                    if (params.documentType != "design") {
                        errors.add("documentType must be 'design' for actionType 'sync_to_tasks'")
                    }
                }
            }
            
            return ValidationResult(errors.isEmpty(), errors)
        }
        
        /**
         * 将参数序列化为 JSON 字符串
         */
        fun serialize(params: WorkflowActionParams): String {
            return gson.toJson(params)
        }
        
        /**
         * 从 JSON 字符串反序列化参数
         */
        fun deserialize(json: String): WorkflowActionParams? {
            return try {
                gson.fromJson(json, WorkflowActionParams::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }
        }
        
        /**
         * 将参数转换为 Map（用于 RPC 调用）
         */
        fun toMap(params: WorkflowActionParams): Map<String, Any?> {
            return mapOf(
                "filePath" to params.filePath,
                "documentType" to params.documentType,
                "lineNumber" to params.lineNumber,
                "startLine" to params.startLine,
                "endLine" to params.endLine,
                "selectedText" to params.selectedText,
                "taskText" to params.taskText,
                "allTasksContent" to params.allTasksContent,
                "requirementsContent" to params.requirementsContent,
                "designContent" to params.designContent,
                "taskStatus" to params.taskStatus,
                "isFirstTask" to params.isFirstTask,
                "actionType" to params.actionType,
                "scopePath" to params.scopePath,
                "diffContent" to params.diffContent
            ).filterValues { it != null }
        }
    }
}

/**
 * 参数验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    val errorMessage: String
        get() = if (isValid) "" else errors.joinToString("; ")
}