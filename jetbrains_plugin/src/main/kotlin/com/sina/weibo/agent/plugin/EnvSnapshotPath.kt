package com.sina.weibo.agent.plugin

import com.intellij.openapi.util.SystemInfo
import java.io.File


object EnvSnapshotPath {

    private const val FILE_NAME = "idea-shell-env.json"

    fun snapshotFile(): File {
        return when {
            SystemInfo.isWindows -> windows()
            SystemInfo.isMac     -> mac()
            else                 -> linux()
        }
    }

    private fun windows(): File {
        val base = System.getenv("LOCALAPPDATA")
            ?: System.getProperty("user.home")
        return File(base, FILE_NAME)
    }

    private fun mac(): File {
        val home = System.getProperty("user.home")
        return File(home, "Library/Caches/$FILE_NAME")
    }

    private fun linux(): File {
        val home = System.getProperty("user.home")
        return File(home, ".cache/$FILE_NAME")
    }
}
