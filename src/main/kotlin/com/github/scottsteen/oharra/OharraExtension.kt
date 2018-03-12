package com.github.scottsteen.oharra

import org.gradle.api.Action
import org.gradle.api.Project

// Extensions must be open in order for Gradle to create Proxy objects
open class OharraExtension(private val project: Project) {

    var versionFile = "gradle.properties"
    var releaseMessage = "[Gradle Oharra plugin] Committing release version"
    var postReleaseMessage = "[Gradle Oharra plugin] Preparing for new development"
    var developmentVersionSuffix = "-SNAPSHOT"

    var scm: ScmConfig = GitConfig(project, this)
        private set

    fun git(configuration: Action<in GitConfig>) {
        val git = GitConfig(project, this)
        scm = git
        configuration.execute(git)
    }

    fun testMode() {
        testMode(Action {})
    }

    fun testMode(configuration: Action<in DisabledConfig>) {
        val disabledConfig = DisabledConfig(project, this)
        scm = disabledConfig
        configuration.execute(disabledConfig)
    }
}

sealed class ScmConfig

class GitConfig(val project: Project, val extension: OharraExtension): ScmConfig() {

    var remote = "origin"

    fun remote(value: String) {
        remote = value
    }
}

class DisabledConfig(val project: Project, val extension: OharraExtension): ScmConfig() {
    private val outputBuilder = mutableListOf<String>()
    val output: List<String>
        get() = outputBuilder

    var printOutput = true

    fun printOutput(value: Boolean) {
        printOutput = value
    }

    fun output(message: String) {
        outputBuilder.add(message.trim())
        if (printOutput) {
            println(message)
        }
    }
}