// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.actions

/**
 * WorkflowActionBase 测试验证文档
 * 
 * 此文件作为测试验证的说明，详细的测试应该通过实际使用来验证
 */
class WorkflowActionBaseTestDoc {
    
    companion object {
        /**
         * 手动测试检查清单
         */
        @JvmStatic
        val MANUAL_TEST_CHECKLIST = listOf(
            "编辑器上下文获取修复验证:",
            "1. 在 .cospec/tasks.md 中点击任务运行按钮 - 应该成功执行",
            "2. 在普通 .java 文件中验证操作不可用 - 应该显示清晰提示",
            "3. 检查 IDE 日志 - 应该包含详细的调试信息",
            "4. 验证 RPC 命令发送 - 应该在日志中看到成功发送的消息",
            "5. 验证 Extension Host 处理 - 不应该出现 JavaScript 空指针错误",
            "",
            "错误信息改进验证:",
            "- 旧错误: 无法获取编辑器上下文",
            "- 新错误: 无法获取编辑器上下文，缺失组件: Editor, PSI File。请确保...",
            "",
            "空指针修复验证:",
            "- Extension Host 不再出现 Cannot read properties of undefined 错误",
            "- 工作流操作能够正常完成整个流程"
        )
        
        /**
         * 测试场景说明
         */
        @JvmStatic
        val TEST_SCENARIOS = mapOf(
            "正常情况" to "在支持的 .cospec/*.md 文件中触发工作流操作",
            "异常情况" to "在不支持的文件中触发，应显示清晰的错误信息", 
            "边界情况" to "编辑器未聚焦、文件类型不匹配等"
        )
        
        /**
         * 预期结果
         */
        @JvmStatic
        val EXPECTED_RESULTS = listOf(
            "不再出现无法获取编辑器上下文的通用错误",
            "显示具体的缺失组件和解决建议",
            "RPC命令能够成功发送到Extension Host",
            "Extension Host不再出现JavaScript空指针错误"
        )
    }
}