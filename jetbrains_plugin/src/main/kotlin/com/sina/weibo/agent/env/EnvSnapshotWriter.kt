// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.env

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString

object EnvSnapshotWriter {

    private val LOG = Logger.getInstance(EnvSnapshotWriter::class.java)
    private const val SNAPSHOT_FILE = "idea-shell-env.json"

    fun ensureSnapshot() {
        try {
            val snapshotPath = resolveSnapshotPath()
            if (snapshotPath.exists()) {
                LOG.info("Env snapshot already exists: ${snapshotPath.pathString}")
                return
            }

            val shell = detectShell()
            val env = collectEnv(shell)

            if (env.isEmpty()) {
                LOG.warn("Collected env is empty, snapshot skipped")
                return
            }

            env["__JETBRAINS_EMBEDDED__"] = "true"

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
