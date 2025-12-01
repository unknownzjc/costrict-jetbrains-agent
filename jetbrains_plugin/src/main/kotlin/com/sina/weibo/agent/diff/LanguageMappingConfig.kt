// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.diff

import com.intellij.openapi.fileTypes.FileTypeManager

/**
 * VSCode语言标识符到IntelliJ文件类型的映射配置
 * 用于在差异视图中实现正确的语法高亮
 */
object LanguageMappingConfig {
    
    /**
     * VSCode语言标识符到文件扩展名的映射
     * 基于VSCode的language标识符系统
     */
    private val vscodeToExtension = mapOf(
        // Web technologies
        "html" to "html",
        "htm" to "html",
        "css" to "css",
        "javascript" to "js",
        "jsx" to "jsx",
        "typescript" to "ts",
        "tsx" to "tsx",
        
        // Backend languages
        "python" to "py",
        "ruby" to "rb",
        "php" to "php",
        "java" to "java",
        "csharp" to "cs",
        "go" to "go",
        "rust" to "rs",
        "scala" to "scala",
        "kotlin" to "kt",
        "swift" to "swift",
        
        // Markup and data
        "json" to "json",
        "xml" to "xml",
        "yaml" to "yaml",
        "yml" to "yaml",
        "markdown" to "md",
        "csv" to "csv",
        
        // Shell and scripting
        "bash" to "sh",
        "shellscript" to "sh",
        "powershell" to "ps1",
        
        // Configuration
        "toml" to "toml",
        "ini" to "ini",
        "cfg" to "ini",
        "conf" to "ini",
        
        // Other
        "sql" to "sql",
        "graphql" to "graphql",
        "gql" to "graphql",
        "latex" to "tex",
        "svg" to "svg",
        "text" to "txt",
        "plaintext" to "txt",
        
        // C-family languages
        "c" to "c",
        "cpp" to "cpp",
        "objective-c" to "m",
        
        // Functional languages
        "haskell" to "hs",
        "elm" to "elm",
        "clojure" to "clj",
        "erlang" to "erl",
        "elixir" to "ex",
        
        // Mobile development
        "dart" to "dart",
        "objective-c" to "m",
        
        // Game development
        "lua" to "lua",
        "gdscript" to "gd",
        
        // Data science and ML
        "r" to "r",
        "julia" to "jl",
        "jupyter" to "ipynb"
    )
    
    /**
     * 特殊文件类型映射 - 直接映射到IntelliJ文件类型名称
     * 用于处理那些不能通过简单扩展名映射的文件类型
     */
    private val specialFileTypeMappings = mapOf(
        "vue" to "Vue.js",
        "svelte" to "Svelte",
        "astro" to "Astro",
        "solid" to "SolidJS"
    )
    
    /**
     * 根据VSCode语言标识符获取对应的文件扩展名
     */
    fun getExtensionFromLanguageId(languageId: String): String? {
        return vscodeToExtension[languageId.lowercase()]
    }
    
    /**
     * 根据文件扩展名获取VSCode语言标识符
     */
    fun getLanguageIdFromExtension(extension: String): String? {
        val lowerExtension = extension.lowercase()
        return vscodeToExtension.entries
            .find { it.value == lowerExtension }
            ?.key
    }
    
    /**
     * 获取特殊文件类型的IntelliJ文件类型名称
     */
    fun getSpecialFileTypeName(languageId: String): String? {
        return specialFileTypeMappings[languageId.lowercase()]
    }
    
    /**
     * 检查是否是特殊文件类型
     */
    fun isSpecialFileType(languageId: String): Boolean {
        return specialFileTypeMappings.containsKey(languageId.lowercase())
    }
    
    /**
     * 获取所有支持的VSCode语言标识符
     */
    fun getSupportedLanguageIds(): Set<String> {
        return vscodeToExtension.keys union specialFileTypeMappings.keys
    }
    
    /**
     * 验证语言标识符是否有效
     */
    fun isValidLanguageId(languageId: String): Boolean {
        return getSupportedLanguageIds().contains(languageId.lowercase())
    }
}