// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.diff

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Base64
import java.nio.charset.StandardCharsets

/**
 * 处理 VSCode "vscode.changes" 命令的 IntelliJ 实现
 * 将 VSCode 的差异视图转换为 IntelliJ 的差异展示
 */
class DiffViewHandler(private val project: Project) {
    private val logger = Logger.getInstance(DiffViewHandler::class.java)
    
    companion object {
        // VSCode 中使用的差异视图 URI 方案
        const val DIFF_VIEW_URI_SCHEME = "costrict-diff"
        // 兼容其他可能的scheme变体
        val COMPATIBLE_SCHEMES = setOf("costrict-diff", "cline-diff", "roo-diff")
    }
    
    /**
     * 处理 "vscode.changes" 命令
     *
     * @param args 命令参数数组，第一个元素是标题，第二个元素是变更数组
     */
    fun handleChangesCommand(args: List<Any?>) {
        if (args.size < 2) {
            logger.warn("❌ vscode.changes 命令参数不足，期望至少2个参数，实际: ${args.size}")
            return
        }
        
        val title = args[0]?.toString() ?: "差异视图"
        @Suppress("UNCHECKED_CAST")
        val changes = args[1] as? List<List<Any>> ?: emptyList()
        logger.info("🔍 处理 vscode.changes 命令: $title, 变更数量: ${changes.size}")
        
        try {
            for ((index, change) in changes.withIndex()) {
                if (change.size < 3) {
                    logger.warn("❌ 变更项格式不正确，期望3个元素，实际: ${change.size}")
                    continue
                }
                
                val originalUri = change[0].toString()
                val beforeUri = change[1].toString()
                val afterUri = change[2].toString()
                
                logger.info("🔍 处理变更 $index: 原文件=$originalUri, Before=$beforeUri, After=$afterUri")
                
                // 检查URI scheme兼容性
                val beforeScheme = beforeUri.substringBefore(":")
                val afterScheme = afterUri.substringBefore(":")
                logger.info("🔍 URI scheme检查 - Before: $beforeScheme, After: $afterScheme")
                
                if (!COMPATIBLE_SCHEMES.contains(beforeScheme)) {
                    logger.warn("⚠️ Before URI使用了未知的scheme: $beforeScheme，期望: $COMPATIBLE_SCHEMES")
                }
                if (!COMPATIBLE_SCHEMES.contains(afterScheme)) {
                    logger.warn("⚠️ After URI使用了未知的scheme: $afterScheme，期望: $COMPATIBLE_SCHEMES")
                }
                
                try {
                    val relativePath = extractRelativePath(beforeUri)
                    val beforeBase64 = extractBase64Content(beforeUri)
                    val afterBase64 = extractBase64Content(afterUri)
                    val extension = extractExtension(relativePath)
                    
                    openChangesDiff(
                        project = project,
                        title = title,
                        relativePath = relativePath,
                        beforeBase64 = beforeBase64,
                        afterBase64 = afterBase64,
                        extension = extension
                    )
                    
                } catch (e: Exception) {
                    logger.error("❌ 处理变更项失败: $index", e)
                }
            }
        } catch (e: Exception) {
            logger.error("❌ 处理 vscode.changes 命令失败", e)
        }
    }
    
    /**
     * 为单个文件打开差异视图
     */
    private fun openChangesDiff(
        project: Project,
        title: String,
        relativePath: String,
        beforeBase64: String,
        afterBase64: String,
        extension: String
    ) {
        try {
            logger.info("🔍 开始Base64解码 - Before长度: ${beforeBase64.length}, After长度: ${afterBase64.length}")
            
            // 添加Base64内容验证
            if (beforeBase64.isEmpty()) {
                logger.warn("⚠️ Before内容的Base64为空")
            }
            if (afterBase64.isEmpty()) {
                logger.warn("⚠️ After内容的Base64为空")
            }
            
            val before = try {
                String(Base64.getDecoder().decode(beforeBase64), StandardCharsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                logger.error("❌ Before内容Base64解码失败: 无效的Base64格式 - ${e.message}, 内容长度: ${beforeBase64.length}", e)
                try {
                    // 尝试修复Base64填充问题后再次解码
                    val fixedBase64 = fixBase64Padding(beforeBase64)
                    String(Base64.getDecoder().decode(fixedBase64), StandardCharsets.UTF_8)
                } catch (e2: Exception) {
                    logger.error("❌ Before内容Base64解码失败(修复后): ${e2.message}", e2)
                    ""
                }
            } catch (e: Exception) {
                logger.error("❌ Before内容Base64解码失败: ${e.message}, 内容长度: ${beforeBase64.length}", e)
                ""
            }
            
            val after = try {
                String(Base64.getDecoder().decode(afterBase64), StandardCharsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                logger.error("❌ After内容Base64解码失败: 无效的Base64格式 - ${e.message}, 内容长度: ${afterBase64.length}", e)
                try {
                    // 尝试修复Base64填充问题后再次解码
                    val fixedBase64 = fixBase64Padding(afterBase64)
                    String(Base64.getDecoder().decode(fixedBase64), StandardCharsets.UTF_8)
                } catch (e2: Exception) {
                    logger.error("❌ After内容Base64解码失败(修复后): ${e2.message}", e2)
                    ""
                }
            } catch (e: Exception) {
                logger.error("❌ After内容Base64解码失败: ${e.message}, 内容长度: ${afterBase64.length}", e)
                ""
            }
            
            logger.info("🔍 解码结果 - Before: '${before.take(50)}...', After: '${after.take(50)}...'")
            logger.info("🔍 解码内容长度 - Before: ${before.length}, After: ${after.length}")
            
            val factory = DiffContentFactory.getInstance()
            val fileTypeManager = FileTypeManager.getInstance()
            
            // 使用增强的文件类型检测方法
            val fileType = detectFileType(extension, relativePath, before)
            
            logger.info("🔍 文件类型检测 - 扩展名: '$extension', 相对路径: '$relativePath', 最终文件类型: ${fileType.name} (描述: ${fileType.description})")
            
            // 确保文件类型正确应用到差异内容
            val beforeContent = factory.create(before, fileType)
            val afterContent = factory.create(after, fileType)
            
            logger.info("🔍 差异内容创建 - Before内容类型: ${beforeContent.contentType?.name ?: "null"}, After内容类型: ${afterContent.contentType?.name ?: "null"}")
            
            // 验证内容类型是否正确设置
            if (beforeContent.contentType?.name != fileType.name) {
                logger.warn("⚠️ Before内容的文件类型不匹配 - 期望: ${fileType.name}, 实际: ${beforeContent.contentType?.name}")
            }
            if (afterContent.contentType?.name != fileType.name) {
                logger.warn("⚠️ After内容的文件类型不匹配 - 期望: ${fileType.name}, 实际: ${afterContent.contentType?.name}")
            }
            
            val request = SimpleDiffRequest(
                "$title — $relativePath",
                beforeContent,
                afterContent,
                "Before",
                "After"
            )
            
            // 检查当前线程是否为 EDT
            val isEdt = ApplicationManager.getApplication().isDispatchThread
            logger.info("🔍 当前线程是否为 EDT: $isEdt")
            
            if (isEdt) {
                // 如果在 EDT 线程中，直接显示差异
                logger.info("🔍 在 EDT 线程中直接显示差异视图")
                DiffManager.getInstance().showDiff(project, request)
            } else {
                // 如果不在 EDT 线程中，使用 invokeLater 切换到 EDT
                logger.info("🔍 不在 EDT 线程中，使用 invokeLater 切换到 EDT 显示差异视图")
                ApplicationManager.getApplication().invokeLater {
                    try {
                        DiffManager.getInstance().showDiff(project, request)
                        logger.info("✅ 成功在 EDT 线程中打开差异视图: $relativePath")
                    } catch (e: Exception) {
                        logger.error("❌ 在 EDT 线程中打开差异视图失败: $relativePath", e)
                    }
                }
            }
            
            logger.info("✅ 成功调度差异视图: $relativePath")
            
        } catch (e: Exception) {
            logger.error("❌ 打开差异视图失败: $relativePath", e)
        }
    }
    
    /**
     * 从 URI 中提取相对路径
     * 格式: ${DIFF_VIEW_URI_SCHEME}:${relativePath}?query
     */
    private fun extractRelativePath(uri: String): String {
        logger.info("🔍 提取相对路径，URI: $uri")
        
        // 首先移除scheme部分，支持多种兼容scheme
        var withoutScheme = uri
        for (scheme in COMPATIBLE_SCHEMES) {
            if (uri.startsWith("$scheme:")) {
                withoutScheme = uri.substringAfter("$scheme:")
                break
            }
        }
        
        if (withoutScheme == uri) {
            logger.warn("⚠️ URI没有使用任何已知的scheme，尝试直接解析: $uri")
        }
        
        val result = withoutScheme.substringBefore("?").ifEmpty {
            logger.warn("⚠️ 无法从URI提取相对路径，使用默认值: $uri")
            "unknown"
        }
        logger.info("🔍 提取的相对路径: $result")
        return result
    }
    
    /**
     * 从 URI 中提取 Base64 编码的内容
     * 支持两种格式：
     * 1. VSCode对象格式：{$mid=1.0, path=index.html, scheme=cline-diff, query=BASE64_CONTENT}
     * 2. 标准URI格式：cline-diff:index.html?BASE64_CONTENT
     */
    private fun extractBase64Content(uri: String): String {
        logger.info("🔍 提取Base64内容，URI: $uri")
        
        // 检查是否是VSCode对象格式（包含{$mid=和query=）
        if (uri.contains("{\$mid=") && uri.contains("query=")) {
            logger.info("🔍 检测到VSCode对象格式，使用正则表达式提取query字段")
            
            // 使用正则表达式提取query字段的值
            val queryPattern = "query=([^}]+)".toRegex()
            val matchResult = queryPattern.find(uri)
            
            if (matchResult != null) {
                var result = matchResult.groupValues[1].trim()
                logger.info("🔍 从VSCode对象格式提取的Base64内容长度: ${result.length}")
                
                // 修复Base64填充问题
                result = fixBase64Padding(result)
                logger.info("🔍 修复Base64填充后长度: ${result.length}")
                
                // 验证Base64格式
                return try {
                    // 尝试解码以验证Base64格式正确性，明确指定UTF-8编码
                    String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8)
                    logger.info("✅ Base64格式验证成功")
                    result
                } catch (e: IllegalArgumentException) {
                    logger.error("❌ Base64格式验证失败: 无效的Base64格式 - ${e.message}", e)
                    try {
                        // 尝试修复Base64填充问题后再次验证
                        val fixedResult = fixBase64Padding(result)
                        String(Base64.getDecoder().decode(fixedResult), StandardCharsets.UTF_8)
                        logger.info("✅ Base64格式验证成功(修复后)")
                        fixedResult
                    } catch (e2: Exception) {
                        logger.error("❌ Base64格式验证失败(修复后): ${e2.message}", e2)
                        ""
                    }
                } catch (e: Exception) {
                    logger.error("❌ Base64格式验证失败: ${e.message}", e)
                    ""
                }
            } else {
                logger.warn("⚠️ 无法从VSCode对象格式中提取query字段")
                return ""
            }
        }
        
        // 回退到标准URI格式解析
        logger.info("🔍 使用标准URI格式解析")
        val queryStart = uri.indexOf("?")
        var result = if (queryStart >= 0 && queryStart < uri.length - 1) {
            uri.substring(queryStart + 1)
        } else {
            logger.warn("⚠️ URI格式不正确，无法提取Base64内容: $uri")
            ""
        }
        
        // 修复Base64填充问题
        result = fixBase64Padding(result)
        logger.info("🔍 提取的Base64内容长度: ${result.length}")
        return result
    }
    
    /**
     * 修复Base64字符串的填充问题
     * Base64字符串长度必须是4的倍数，如果不是则添加适当的填充字符
     */
    private fun fixBase64Padding(base64String: String): String {
        if (base64String.isEmpty()) return base64String
        
        val remainder = base64String.length % 4
        return if (remainder == 0) {
            // 长度正确，无需修复
            base64String
        } else {
            // 需要添加填充字符
            val paddingNeeded = 4 - remainder
            val fixedString = base64String + "=".repeat(paddingNeeded)
            logger.info("🔍 Base64填充修复: 原长度=${base64String.length}, 新长度=${fixedString.length}, 添加填充=${paddingNeeded}")
            fixedString
        }
    }
    
    /**
     * 增强的文件类型检测方法
     * 结合扩展名、文件名模式和内容特征来准确识别文件类型
     * 集成VSCode语言映射配置以实现正确的语法高亮
     */
    private fun detectFileType(extension: String, filePath: String, content: String): com.intellij.openapi.fileTypes.FileType {
        val fileTypeManager = FileTypeManager.getInstance()
        
        // 0. VSCode语言映射检测 - 新添加的层级
        val vsCodeLanguageId = LanguageMappingConfig.getLanguageIdFromExtension(extension)
        if (vsCodeLanguageId != null) {
            // 检查是否是特殊文件类型（如Vue、Svelte等）
            if (LanguageMappingConfig.isSpecialFileType(vsCodeLanguageId)) {
                val specialFileTypeName = LanguageMappingConfig.getSpecialFileTypeName(vsCodeLanguageId)
                if (specialFileTypeName != null) {
                    val specialFileType = fileTypeManager.findFileTypeByName(specialFileTypeName)
                    if (specialFileType != null && specialFileType.name != "UNKNOWN") {
                        logger.info("🔍 步骤0 - VSCode特殊文件类型映射: ${specialFileType.name} (语言ID: '$vsCodeLanguageId')")
                        return specialFileType
                    }
                }
            }
            
            // 尝试通过VSCode映射的扩展名获取文件类型
            val mappedExtension = LanguageMappingConfig.getExtensionFromLanguageId(vsCodeLanguageId)
            if (mappedExtension != null) {
                val mappedFileType = fileTypeManager.getFileTypeByExtension(mappedExtension)
                if (mappedFileType.name != "UNKNOWN" && mappedFileType.name != "PLAIN_TEXT") {
                    logger.info("🔍 步骤0 - VSCode语言映射检测: ${mappedFileType.name} (语言ID: '$vsCodeLanguageId', 映射扩展名: '$mappedExtension')")
                    return mappedFileType
                }
            }
        }
        
        // 1. 首先尝试通过扩展名检测
        var fileType = fileTypeManager.getFileTypeByExtension(extension)
        logger.info("🔍 步骤1 - 扩展名检测: ${fileType.name} (扩展名: '$extension')")
        
        // 2. 如果扩展名检测失败或为UNKNOWN，尝试文件名模式
        if (fileType.name == "UNKNOWN" || fileType.name == "PLAIN_TEXT") {
            fileType = fileTypeManager.getFileTypeByFileName(filePath)
            logger.info("🔍 步骤2 - 文件名检测: ${fileType.name} (路径: '$filePath')")
        }
        
        // 3. 特殊处理常见的前端文件类型（现在通过VSCode映射处理）
        if (fileType.name == "UNKNOWN" || fileType.name == "PLAIN_TEXT") {
            fileType = when (extension.lowercase()) {
                "tsx" -> fileTypeManager.getFileTypeByExtension("typescript")
                "jsx" -> fileTypeManager.getFileTypeByExtension("javascript")
                else -> fileType
            }
            logger.info("🔍 步骤3 - 传统特殊文件类型处理: ${fileType.name} (扩展名: '$extension')")
        }
        
        // 4. 内容启发式检测 - 检查文件内容特征
        if (fileType.name == "UNKNOWN" || fileType.name == "PLAIN_TEXT") {
            fileType = detectFileTypeByContent(content, extension, filePath)
            logger.info("🔍 步骤4 - 内容启发式检测: ${fileType.name}")
        }
        
        // 5. 如果仍然无法识别，使用更智能的回退策略
        if (fileType.name == "UNKNOWN" || fileType.name == "PLAIN_TEXT") {
            fileType = getSmartFallbackFileType(extension, filePath)
            logger.info("🔍 步骤5 - 智能回退: ${fileType.name}")
        }
        
        return fileType
    }
    
    /**
     * 基于内容特征检测文件类型
     */
    private fun detectFileTypeByContent(content: String, extension: String, filePath: String): com.intellij.openapi.fileTypes.FileType {
        val fileTypeManager = FileTypeManager.getInstance()
        
        // 检查是否是HTML/XML
        if (content.trimStart().startsWith("<!DOCTYPE html") ||
            content.trimStart().startsWith("<html") ||
            (content.contains("<html") && content.contains("</html>"))) {
            return fileTypeManager.getFileTypeByExtension("html")
        }
        
        // 检查是否是JSON
        if (content.trimStart().startsWith("{") && content.trim().endsWith("}") ||
            content.trimStart().startsWith("[") && content.trim().endsWith("]")) {
            return try {
                // 简单的JSON验证
                com.google.gson.JsonParser.parseString(content)
                logger.info("🔍 内容检测为JSON格式")
                fileTypeManager.getFileTypeByExtension("json")
            } catch (e: Exception) {
                logger.info("🔍 内容不是有效的JSON格式")
                fileTypeManager.getFileTypeByExtension(extension)
            }
        }
        
        // 检查是否是Markdown
        if (content.contains("# ") || content.contains("## ") || content.contains("```")) {
            return fileTypeManager.getFileTypeByExtension("md")
        }
        
        // 检查是否是YAML
        if (content.contains(": ") && !content.contains("{") && extension in setOf("yml", "yaml")) {
            return fileTypeManager.getFileTypeByExtension("yaml")
        }
        
        // 检查是否是配置文件
        if (filePath.contains("config") || filePath.contains(".env") || filePath.contains("package.json")) {
            when (extension) {
                "js" -> return fileTypeManager.getFileTypeByExtension("javascript")
                "ts" -> return fileTypeManager.getFileTypeByExtension("typescript")
            }
        }
        
        return fileTypeManager.getFileTypeByExtension(extension)
    }
    
    /**
     * 智能回退文件类型检测
     */
    private fun getSmartFallbackFileType(extension: String, filePath: String): com.intellij.openapi.fileTypes.FileType {
        val fileTypeManager = FileTypeManager.getInstance()
        
        // 基于文件路径模式的智能检测
        return when {
            filePath.contains("/src/") && extension in setOf("js", "ts", "jsx", "tsx") -> {
                // 源码目录中的JS/TS文件
                fileTypeManager.getFileTypeByExtension(if (extension.endsWith("x")) "javascript" else extension)
            }
            filePath.contains("test") && extension in setOf("js", "ts") -> {
                // 测试文件
                fileTypeManager.getFileTypeByExtension(extension)
            }
            filePath.contains("package.json") -> fileTypeManager.getFileTypeByExtension("json")
            filePath.endsWith("README") -> fileTypeManager.getFileTypeByExtension("txt")
            else -> fileTypeManager.getFileTypeByExtension(extension)
        }
    }
    
    /**
     * 从文件路径中提取扩展名，支持复合扩展名
     * 例如: .tsx, .jsx, .test.ts, .spec.js
     */
    private fun extractExtension(filePath: String): String {
        val fileName = filePath.substringAfterLast('/')
        val lastDotIndex = fileName.lastIndexOf('.')
        
        return if (lastDotIndex >= 0 && lastDotIndex < fileName.length - 1) {
            val extension = fileName.substring(lastDotIndex + 1).lowercase()
            
            // 检查是否有复合扩展名
            val nameWithoutLastExt = fileName.substring(0, lastDotIndex)
            val secondLastDotIndex = nameWithoutLastExt.lastIndexOf('.')
            
            val finalExtension = if (secondLastDotIndex >= 0) {
                val secondExt = nameWithoutLastExt.substring(secondLastDotIndex + 1).lowercase()
                // 常见的复合扩展名模式
                when {
                    secondExt == "test" && extension in setOf("js", "ts", "jsx", "tsx") -> extension
                    secondExt == "spec" && extension in setOf("js", "ts", "jsx", "tsx") -> extension
                    secondExt in setOf("js", "ts") && extension in setOf("x") -> "$secondExt$extension"
                    else -> extension
                }
            } else {
                extension
            }
            
            logger.info("🔍 扩展名提取 - 文件路径: '$filePath', 文件名: '$fileName', 提取的扩展名: '$finalExtension'")
            finalExtension
        } else {
            logger.info("🔍 扩展名提取 - 文件路径: '$filePath', 未找到扩展名，使用默认值: 'txt'")
            "txt" // 默认为文本文件
        }
    }
}