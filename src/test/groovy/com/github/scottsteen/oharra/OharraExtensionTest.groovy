package com.github.scottsteen.oharra

import org.gradle.api.Project
import spock.lang.Specification

class OharraExtensionTest extends Specification{

    def 'should enable git scm when closure last invoked'() {
        given:
        def extension = new OharraExtension(Mock(Project))

        when:
        extension.testMode()
        extension.git {}

        then:
        extension.scm.class == GitConfig
    }
}
