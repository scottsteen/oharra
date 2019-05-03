package com.github.scottsteen.oharra

import org.gradle.api.Plugin
import org.gradle.api.Project

class OharraPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create("oharra", OharraExtension::class.java, project)
        project.tasks.create("release", OharraReleaseTask::class.java)

        val currentVersionProvider = project.provider { project.version }
        if (project.isRoot()) {
            project.allprojects.forEach {
                it.afterEvaluate { p ->
                    p.version = project.findProperty("releaseVersion")
                            ?: currentVersionProvider.get().toString().substringBefore(extension.developmentVersionSuffix)
                }
            }
        }
    }

    private fun Project.isRoot() = this == this.rootProject
}

class SCMFactory {

    companion object {
        fun scm(extension: OharraExtension): SCM {

            return when (extension.scm) {
                is GitConfig -> Git(extension.scm as GitConfig)
                is DisabledConfig -> NoOpSCM(extension.scm as DisabledConfig)
            }
        }
    }
}