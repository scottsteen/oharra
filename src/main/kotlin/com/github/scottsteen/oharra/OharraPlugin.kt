package com.github.scottsteen.oharra

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.Cast.uncheckedCast
import java.lang.String.format

class OharraPlugin : Plugin<Project> {

    private val releaseTaskArgs = mapOf(
            "group" to "release",
            "description" to "Performs a release of your project")

    override fun apply(project: Project) {

        val extension = project.extensions.create("oharra", OharraExtension::class.java, project)

        val prevVersion = project.version
        if (project.isRoot()) {
            val releaseVersion = project.provider {
                project.findProperty("releaseVersion")
                        ?: prevVersion.toString().substringBefore(extension.developmentVersionSuffix)
            }

            project.allprojects.forEach {
                it.afterEvaluate {
                    it.version = releaseVersion.get()
                }
            }
        }

        project.task("release", releaseTaskArgs) {

            doFirst {
                if (prevVersion == project.version) {
                    throw GradleException("The release version cannot be the same as the current version")
                }
            }

            onlyIf {
                project.isRoot()
            }

            doLast {

                val scm = SCMFactory.scm(extension)

                project.writeReleaseVersion()
                scm.release()

                project.writeDevelopmentVersion {
                    val versionRegex = Regex("""((?:\d+\.)+)(\d)""")
                    versionRegex.replace(version.toString()) {
                        val (prefix, incrementalVal) = it.destructured
                        val nextVersion = incrementalVal.toInt() + 1
                        "$prefix$nextVersion"
                    }
                }
                scm.postRelease()
            }
        }
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
                .joinToString(format("%n")) {
                    versionRegex.replace(it) {
                        val (key) = it.destructured
                        "$key$newVersion"
                    }
                }
        file(extension.versionFile).writeText(newFileContents)
    }

    private fun Project.task(name: String, args: Map<String, *>, configure: Task.() -> Unit) =
            this.task(args, name, delegateClosureOf(configure))

    private fun <T> delegateClosureOf(action: T.() -> Unit) =
            object : Closure<Unit>(this, this) {
                @Suppress("unused") // to be called dynamically by Groovy
                fun doCall() = uncheckedCast<T>(delegate).action()
            }

    private fun Project.isRoot() = this == this.rootProject
}

class SCMFactory {

    companion object {
        fun scm(extension: OharraExtension): SCM {

            return when (extension.scm) {
                is GitConfig -> Git(extension.scm as GitConfig)
                is DisabledConfig -> NoOpSCM(extension)
            }
        }
    }
}