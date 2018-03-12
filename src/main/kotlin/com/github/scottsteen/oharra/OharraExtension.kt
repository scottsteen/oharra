package com.github.scottsteen.oharra

import org.gradle.api.Action
import org.gradle.api.Project

// Extensions must be open in order for Gradle to create Proxy objects
open class OharraExtension(internal val project: Project) {

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

    fun scmDisabled() {
        scm = DisabledConfig
    }
}

sealed class ScmConfig

class GitConfig(val project: Project, val extension: OharraExtension): ScmConfig() {

    var remote = "origin"

    fun remote(value: String) {
        remote = value
    }
}

object DisabledConfig: ScmConfig()