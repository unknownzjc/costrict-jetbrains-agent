// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.env

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readText

object EnvSnapshotWriter {

    private val LOG = Logger.getInstance(EnvSnapshotWriter::class.java)
    private const val SNAPSHOT_FILE = "idea-shell-env.json"
    private const val UPDATE_TIME_KEY = "__UPDATE_TIME__"
    private val MIN_REFRESH_INTERVAL_MINUTES = 5L

    fun ensureSnapshot() {
        try {
            val snapshotPath = resolveSnapshotPath()
            
            // 检查文件是否存在且更新时间是否超过5分钟
            if (snapshotPath.exists()) {
                val content = snapshotPath.readText()
                if (content.isNotEmpty()) {
                    val env = parseJson(content)
                    val updateTimeStr = env[UPDATE_TIME_KEY]
                    
                    if (updateTimeStr != null) {
                        try {
                            val updateTime = Instant.parse(updateTimeStr)
                            val now = Instant.now()
                            val minutesSinceUpdate = ChronoUnit.MINUTES.between(updateTime, now)
                            
                            if (minutesSinceUpdate < MIN_REFRESH_INTERVAL_MINUTES) {
                                LOG.info("Env snapshot is recent (${minutesSinceUpdate} minutes ago), skipping refresh: ${snapshotPath.pathString}")
                                return
                            } else {
                                LOG.info("Env snapshot is outdated (${minutesSinceUpdate} minutes ago), refreshing: ${snapshotPath.pathString}")
                            }
                        } catch (e: Exception) {
                            LOG.warn("Failed to parse update time from snapshot, will refresh: ${e.message}")
                        }
                    } else {
                        LOG.info("Env snapshot missing update time, will refresh: ${snapshotPath.pathString}")
                    }
                }
            }

            val shell = detectShell()
            val env = collectEnv(shell)

            if (env.isEmpty()) {
                LOG.warn("Collected env is empty, snapshot skipped")
                return
            }

            env["__JETBRAINS_EMBEDDED__"] = "true"
            env[UPDATE_TIME_KEY] = Instant.now().toString()

            writeJson(snapshotPath.toFile(), env)

            LOG.info("Env snapshot written: ${snapshotPath.pathString}, size=${env.size}")
        } catch (e: Exception) {
            LOG.error("Failed to write env snapshot", e)
        }
    }

    // ------------------------------------------------------------
    // Shell detection
    // ------------------------------------------------------------

    private fun detectShell(): Shell {
        val os = System.getProperty("os.name").lowercase()

        if (os.contains("win")) {
            val comspec = System.getenv("COMSPEC") ?: ""
            if (comspec.contains("cmd", ignoreCase = true)) {
                return Shell.CMD
            }
            return Shell.POWERSHELL
        }

        val shellEnv = System.getenv("SHELL") ?: ""
        return when {
            shellEnv.contains("zsh") -> Shell.ZSH
            else -> Shell.BASH
        }
    }

    // ------------------------------------------------------------
    // Env collection
    // ------------------------------------------------------------

    private fun collectEnv(shell: Shell): MutableMap<String, String> {
        val processBuilder =
                when (shell) {
                    Shell.BASH ->
                            ProcessBuilder(
                                    "bash",
                                    "-l",
                                    "-c",
                                    "source ~/.bashrc >/dev/null 2>&1; env"
                            )
                    Shell.ZSH ->
                            ProcessBuilder(
                                    "zsh",
                                    "-l",
                                    "-c",
                                    "source ~/.zshrc >/dev/null 2>&1; env"
                            )
                    Shell.POWERSHELL ->
                            ProcessBuilder(
                                    "powershell",
                                    "-NoProfile",
                                    "-NonInteractive",
                                    "-Command",
                                    "[Console]::OutputEncoding=[Text.Encoding]::UTF8; Get-ChildItem Env: | ForEach-Object { \"\$(\$_.Name)=\$(\$_.Value)\" }"
                            )
                    Shell.CMD -> ProcessBuilder("cmd", "/d", "/c", "set")
                }

        val process = processBuilder.start()
        val output = process.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        process.waitFor()

        return parseEnv(output)
    }

    private fun parseEnv(output: String): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()

        output.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.contains('=') }
                .forEach { line ->
                    val idx = line.indexOf('=')
                    val key = line.substring(0, idx)
                    val value = line.substring(idx + 1)
                    map[key] = value
                }

        return map
    }

    private fun parseJson(content: String): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        
        try {
            // 简单的JSON解析，假设格式为 {"key1":"value1","key2":"value2"}
            val trimmed = content.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val inner = trimmed.substring(1, trimmed.length - 1)
                val pairs = inner.split(",")
                
                for (pair in pairs) {
                    val keyValue = pair.split(":", limit = 2)
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim().removeSurrounding("\"")
                        val value = keyValue[1].trim().removeSurrounding("\"")
                        map[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to parse JSON content: ${e.message}")
        }
        
        return map
    }

    // ------------------------------------------------------------
    // Snapshot path
    // ------------------------------------------------------------

    private fun resolveSnapshotPath(): java.nio.file.Path {
        val os = System.getProperty("os.name").lowercase()

        return when {
            os.contains("win") -> {
                val base =
                        System.getenv("LOCALAPPDATA")
                                ?: throw IllegalStateException("LOCALAPPDATA not set")
                File(base, SNAPSHOT_FILE).toPath()
            }
            os.contains("mac") -> {
                File(System.getProperty("user.home"), "Library/Caches/$SNAPSHOT_FILE").toPath()
            }
            else -> {
                File(System.getProperty("user.home"), ".cache/$SNAPSHOT_FILE").toPath()
            }
        }.also { it.parent?.createDirectories() }
    }

    // ------------------------------------------------------------
    // JSON writer
    // ------------------------------------------------------------

    private fun writeJson(file: File, env: Map<String, String>) {
        val json = buildString {
            append("{")
            env.entries.forEachIndexed { index, entry ->
                if (index > 0) append(",")
                append("\"")
                append(escape(entry.key))
                append("\":\"")
                append(escape(entry.value))
                append("\"")
            }
            append("}")
        }

        Files.writeString(
                file.toPath(),
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW
        )
    }

    private fun escape(s: String): String =
            s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}

private enum class Shell {
    BASH,
    ZSH,
    POWERSHELL,
    CMD
}
