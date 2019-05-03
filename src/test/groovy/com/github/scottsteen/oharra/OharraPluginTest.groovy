package com.github.scottsteen.oharra

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class OharraPluginTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    Project project
    OharraExtension extension

    def setup() {
        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir.root)
                .build()
        project.apply plugin: 'oharra'
        extension = project.extensions.getByType(OharraExtension)
        extension.releaseMessage = ""
        extension.postReleaseMessage = ""
        extension.testMode {
            printOutput false
        }

        def versionFile = project.file('gradle.properties')
        versionFile << "version=${project.version}"
    }

    def 'should commit release version to scm'() {
        given:
        project.with {
            version = '0.1.0-SNAPSHOT'
            ext.releaseVersion = '1.0.0'
            ext.developmentVersion = '1.1.0-SNAPSHOT'
        }

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        extension.scm.output[0] == "[1.0.0]"
    }

    def 'should commit next development version to scm'() {
        given:
        project.with {
            version = '0.1.0-SNAPSHOT'
            ext.developmentVersion = '1.1.0-SNAPSHOT'
        }

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        extension.scm.output[1] == "[1.1.0-SNAPSHOT]"
    }

    def 'should write next development version to properties file'() {
        given:
        project.with {
            version = '0.1.0-SNAPSHOT'
            ext.developmentVersion = '1.1.0-SNAPSHOT'
        }
        def versionFile = project.file('gradle.properties')
        versionFile << "version=${project.version}"

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        versionFile.readLines() == ['version=1.1.0-SNAPSHOT']
    }

    def 'should automatically determine release version'() {
        given:
        project.with {
            version = '1.0.0-SNAPSHOT'
        }
        def versionFile = project.file('gradle.properties')
        versionFile << "version=${project.version}"

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        extension.scm.output[0] == "[1.0.0]"
    }

    def 'should automatically determine next development version'() {
        given:
        project.with {
            ext.releaseVersion = '1.1.0'
        }
        def versionFile = project.file('gradle.properties')
        versionFile << "version=${project.version}"

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        extension.scm.output[1] == "[1.2.0-SNAPSHOT]"
    }

    def 'should accept release version with suffix'() {
        given:
        project.with {
            ext.releaseVersion = '1.1.0.RELEASE'
        }
        def versionFile = project.file('gradle.properties')
        versionFile << "version=${project.version}"

        when:
        project.evaluate()
        project.tasks.release.release()

        then:
        extension.scm.output[0] == "[1.1.0.RELEASE]"
        extension.scm.output[1] == "[1.2.0-SNAPSHOT]"
    }
}