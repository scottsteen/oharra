package com.github.scottsteen.oharra

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

interface SCM {

    fun release()

    fun postRelease()

}

class Git(private val config: GitConfig) : SCM {

    override fun release() {

        val extension = config.extension
        val versionFile = extension.versionFile

        val project = config.project
        project.git("add", versionFile)
        project.git("commit", "-m", "${extension.releaseMessage} [${project.version}]")
        project.git("tag", project.version)
    }

    override fun postRelease() {

        val extension = config.extension
        val versionFile = extension.versionFile

        val project = config.project
        project.git("add", versionFile)
        project.git("commit", "-m", "${extension.postReleaseMessage} [${project.version}]")

        val branch = project.git("rev-parse", "--abbrev-ref", "HEAD")
        project.git("push", config.remote, branch, "--tags")
    }

    private fun Project.git(vararg args: Any): String {

        val out = ByteArrayOutputStream()
        val result = exec { spec ->
            spec.commandLine("git", *args)
            spec.standardOutput = out
            spec.errorOutput = out
            spec.isIgnoreExitValue = true
        }

        val outText = out.toString(UTF_8.name()).trim()
        if (result.exitValue != 0) {
            throw GradleException(outText)
        }

        return outText
    }
}

class NoOpSCM(private val config: DisabledConfig) : SCM {

    override fun release() {
        outputMessage(config.extension.releaseMessage)
    }

    override fun postRelease() {
        outputMessage(config.extension.postReleaseMessage)
    }

    private fun outputMessage(message: String) {
        val project = config.project
        config.output("$message [${project.version}]")
    }
}