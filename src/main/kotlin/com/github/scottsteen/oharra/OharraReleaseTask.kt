package com.github.scottsteen.oharra

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

// Tasks must be open in order for Gradle to create Proxy objects
open class OharraReleaseTask @Inject constructor() : DefaultTask() {

    private val currentVersion = project.version

    init {
        group = "release"
        description = "Performs a release of your project"
        onlyIf {
            project.isRoot()
        }
        doFirst {
            if (currentVersion == project.version) {
                throw GradleException("The release version [${project.version}] cannot be the same as the current version.")
            }
        }
    }

    @TaskAction
    fun release() {
        val extension = project.extensions.getByType(OharraExtension::class.java)
        val scm = SCMFactory.scm(extension)

        project.writeReleaseVersion()
        scm.release()

        project.writeDevelopmentVersion {
            val versionRegex = Regex("""(\d+\.)(\d)\.\d.*""")
            versionRegex.replace(version.toString()) {
                val (prefix, incrementalVal) = it.destructured
                val nextVersion = incrementalVal.toInt() + 1
                "$prefix$nextVersion.0"
            }
        }
        scm.postRelease()
    }

    private fun Project.writeReleaseVersion() {
        val extension = extensions.getByType(OharraExtension::class.java)
        writeVersion(version, extension)
    }

    private fun Project.writeDevelopmentVersion(default: Project.() -> Any) {
        val extension = extensions.getByType(OharraExtension::class.java)
        version = findProperty("developmentVersion") ?: default()

        if (!version.toString().endsWith(extension.developmentVersionSuffix)) {
            version = "$version${extension.developmentVersionSuffix}"
        }

        writeVersion(version, extension)
    }

    private fun Project.writeVersion(newVersion: Any, extension: OharraExtension) {

        val versionRegex = Regex("""^(version\s*(?:=\s*)?).+$""")
        val newFileContents = file(extension.versionFile)
                .bufferedReader()
                .lineSequence()
                .joinToString(System.lineSeparator()) {
                    versionRegex.replace(it) {
                        val (key) = it.destructured
                        "$key$newVersion"
                    }
                }
        file(extension.versionFile).writeText(newFileContents)
    }

    private fun Project.isRoot() = this == this.rootProject
}