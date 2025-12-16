package com.sina.weibo.agent.plugin

object EnvWhitelist {

    private val EXACT_KEYS = setOf(
        "PATH",
        "JAVA_HOME",
        "GOROOT",
        "GOPATH",
        "LANG",
        "LC_ALL"
    )

    private val PREFIX_KEYS = listOf(
        "NVM_",
        "PYENV_",
        "SDKMAN_",
        "CONDA_",
        "BUN_",
        "CARGO_",
        "VSCODE_",
        "GIT_",
        "GEMINI_CLI_",
        "ZGSM_",
        "ANTHROPIC_",
        "JETBRAINS_",
        "GEMINI_",
        "LC_",
        "RUST_"
    )

    fun filter(env: Map<String, String>): Map<String, String> {
        return env.filter { (key, _) ->
            key in EXACT_KEYS || PREFIX_KEYS.any { key.startsWith(it) }
        }
    }
}
