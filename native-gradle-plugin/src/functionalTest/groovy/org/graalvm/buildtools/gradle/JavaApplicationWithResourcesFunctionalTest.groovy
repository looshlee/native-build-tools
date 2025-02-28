/*
 * Copyright (c) 2021, 2021 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.buildtools.gradle

import org.graalvm.buildtools.gradle.fixtures.AbstractFunctionalTest
import spock.lang.Unroll

class JavaApplicationWithResourcesFunctionalTest extends AbstractFunctionalTest {
    @Unroll("can build an application which uses resources using #pattern on Gradle #version with JUnit Platform #junitVersion")
    def "can build an application which uses resources"() {
        gradleVersion = version
        def nativeApp = file("build/native/nativeBuild/java-application")
        debug = true
        given:
        withSample("java-application-with-resources")

        buildFile << config

        when:
        run 'nativeBuild'

        then:
        tasks {
            succeeded ':jar', ':nativeBuild'
            doesNotContain ':build', ':run'
        }

        and:
        nativeApp.exists()

        when:
        def process = execute(nativeApp)

        then:
        process.output.contains "Hello, native!"

        and:
        file("build/native/generated/generateResourcesConfigFile/resource-config.json").text == '''{
  "resources" : {
    "includes" : [ {
      "pattern" : "\\\\Qmessage.txt\\\\E"
    } ],
    "excludes" : [ ]
  },
  "bundles" : [ ]
}'''

        where:
        junitVersion = System.getProperty('versions.junit')
        [version, [pattern, config]] << [TESTED_GRADLE_VERSIONS,
                                         [["explicit resource declaration", """
nativeBuild {
    resources {
        includedPatterns.add(java.util.regex.Pattern.quote("message.txt"))
    }
}
"""],
                                         ["detected", """
nativeBuild {
    resources {
        autodetection {
            enabled = true
            restrictToProjectDependencies = false
            detectionExclusionPatterns.add("META-INF/.*")
        }
    }
}
"""
                                         ],
                                          ["project local detection only", """
nativeBuild {
    resources {
        autodetection {
            enabled = true
            restrictToProjectDependencies = true
        }
    }
}
"""
                                          ]]
        ].combinations()
    }

    @Unroll("can run native tests which uses resources using #pattern on Gradle #version with JUnit Platform #junitVersion")
    def "can run native tests which uses resources"() {
        gradleVersion = version

        given:
        withSample("java-application-with-resources")

        buildFile << config

        when:
        run 'nativeTest'

        then:
        tasks {
            succeeded ':jar', ':nativeTest'
            doesNotContain ':build', ':run'
        }

        and:
        file("build/native/generated/generateTestResourcesConfigFile/resource-config.json").text == '''{
  "resources" : {
    "includes" : [ {
      "pattern" : "\\\\Qmessage.txt\\\\E"
    }, {
      "pattern" : "\\\\Qorg/graalvm/demo/expected.txt\\\\E"
    } ],
    "excludes" : [ ]
  },
  "bundles" : [ ]
}'''

        where:
        junitVersion = System.getProperty('versions.junit')
        [version, [pattern, config]] << [TESTED_GRADLE_VERSIONS,
                                         [["explicit resource declaration", """
nativeTest {
    resources {
        includedPatterns.add(java.util.regex.Pattern.quote("message.txt"))
        includedPatterns.add(java.util.regex.Pattern.quote("org/graalvm/demo/expected.txt"))
    }
}
"""],
                                         ["detected", """
nativeTest {
    resources {
        autodetection {
            enabled = true
            detectionExclusionPatterns.addAll("META-INF/.*", "junit-platform-unique-ids.*")
        }
    }
}
"""
                                         ]]
        ].combinations()
    }
}
