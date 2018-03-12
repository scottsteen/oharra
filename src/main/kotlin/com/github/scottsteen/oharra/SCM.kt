package com.github.scottsteen.oharra

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.ByteArrayOutputStream

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

        val outText = out.toString("UTF-8").trim()
        if (result.exitValue != 0) {
            throw GradleException(outText)
        }

        return outText
    }
}

class NoOpSCM(private val extension: OharraExtension) : SCM {

    override fun release() {
        val project = extension.project
        println("${extension.releaseMessage} [${project.version}]")
    }

    override fun postRelease() {
        val project = extension.project
        println("${extension.postReleaseMessage} [${project.version}]")
    }
}