package com.github.scttsteen.oharra

import com.github.scottsteen.oharra.Git
import com.github.scottsteen.oharra.GitConfig
import com.github.scottsteen.oharra.OharraExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Requires
import spock.lang.Specification

// Requires Git to be available on the PATH
@Requires({ env.Path =~ '[gG][iI][tT]' })
class GitTest extends Specification {

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    File local
    File remote

    def setup() {
        local = temporaryFolder.newFolder()
        remote = temporaryFolder.newFolder()

        "git init --bare".execute([], remote).waitFor()
        "git init".execute([], local).waitFor()
        "git remote add origin $remote".execute([], local).waitFor()
        "git config user.email \"git@test.com\"".execute([], local).waitFor()
        "git config user.name \"git test\"".execute([], local).waitFor()
    }

    def 'should commit and tag only release version'() {
        given:
        def project = ProjectBuilder.builder().withProjectDir(local).build()
        project.version = '0.0.1'
        def extension = new OharraExtension(project)
        extension.releaseMessage = 'release'
        project.file(extension.versionFile) << 'version=0.0.1'
        project.file('.gitignore') << """*
                                              |!.gitignore
                                              |!${extension.versionFile}""".stripMargin()
        def scm = new Git(new GitConfig(project, extension))

        when:
        scm.release()

        then:
        message(local) == 'release [0.0.1]'
        status(local) == '?? .gitignore'
        diff(local) == 'gradle.properties'
        tags(local) == '0.0.1'
    }

    def 'should commit only next development version'() {
        given:
        def project = ProjectBuilder.builder().withProjectDir(local).build()
        project.version = '0.1.0-SNAPSHOT'
        def extension = new OharraExtension(project)
        extension.postReleaseMessage = 'post release'
        project.file(extension.versionFile) << 'version=0.1.0-SNAPSHOT'
        project.file('.gitignore') << """*
                                              |!.gitignore
                                              |!${extension.versionFile}""".stripMargin()
        def scm = new Git(new GitConfig(project, extension))

        when:
        git "add .gitignore", local
        git "commit -m \"initial commit\"", local
        scm.postRelease()

        then:
        message(local) == 'post release [0.1.0-SNAPSHOT]'
        diff(local) == 'gradle.properties'
    }

    def 'should push commits and tag to remote'() {
        given:
        def project = ProjectBuilder.builder().withProjectDir(local).build()
        project.version = '0.0.1'
        def extension = new OharraExtension(project)
        project.file(extension.versionFile) << 'version=0.0.1'
        project.file('.gitignore') << """*
                                              |!.gitignore
                                              |!${extension.versionFile}""".stripMargin()
        def scm = new Git(new GitConfig(project, extension))

        when:
        git "add .gitignore", local
        git "commit -m \"initial commit\"", local
        git "tag 0.0.1", local
        scm.postRelease()

        then:
        hash(remote) == hash(local)
        tags(remote) == tags(local)
    }

    def message(dir) {
        git "log -1 --format=%s", dir
    }

    def status(dir) {
        git "status -s", dir
    }

    def diff(dir) {
        git "log -1 --format= --name-only", dir
    }

    def hash(dir) {
        git "log -1 --format=%H", dir
    }

    def tags(dir) {
        git "tag", dir
    }

    def git(command, dir) {
        def process = "git $command".execute(null, dir)
        def out = new ByteArrayOutputStream()
        process.waitForProcessOutput(out, out)
        out.toString("UTF-8").trim()
    }
}
