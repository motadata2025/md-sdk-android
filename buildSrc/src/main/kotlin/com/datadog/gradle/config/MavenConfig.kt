/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.motadata.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.plugins.signing.SigningExtension

object MavenConfig {
    const val GROUP_ID = "com.motadata"
    const val PUBLICATION = "release"
}

fun Project.publishingConfig(
    projectDescription: String,
    customArtifactId: String = name
) {
    val projectName = name

    val androidExtension =
        extensions.findByType(LibraryExtension::class.java)
    if (androidExtension == null) {
        logger.error("Missing android library extension for $projectName")
        return
    }

    androidExtension.publishing {
        singleVariant(MavenConfig.PUBLICATION) {
            withSourcesJar()
            withJavadocJar()
        }
    }

    afterEvaluate {
        val publishingExtension = extensions.findByType(PublishingExtension::class)
        val signingExtension = extensions.findByType(SigningExtension::class)
        if (publishingExtension == null || signingExtension == null) {
            logger.error("Missing publishing or signing extension for $projectName")
            return@afterEvaluate
        }

        publishingExtension.apply {
            publications.create<MavenPublication>(MavenConfig.PUBLICATION) {
                from(components.getByName("release"))

                groupId = MavenConfig.GROUP_ID
                artifactId = customArtifactId
                version = AndroidConfig.VERSION.name

                pom {
                    name.set(customArtifactId)
                    description.set(projectDescription)
                    url.set("https://github.com/motadata2025/md-sdk-android/")

                    licenses {
                        license {
                            name.set("Apache-2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    organization {
                        name.set("Motadata")
                        url.set("https://www.motadata.com/")
                    }

                    developers {
                        developer {
                            name.set("Motadata")
                            email.set("info@motadata.com")
                            organization.set("Motadata")
                            organizationUrl.set("https://www.motadata.com/")
                        }
                    }

                    scm {
                        url.set("https://github.com/motadata2025/md-sdk-android/")
                        connection.set("scm:git:git@github.com:motadata2025/md-sdk-android.git")
                        developerConnection.set("scm:git:git@github.com:motadata2025/md-sdk-android.git")
                    }
                }
            }
        }

        signingExtension.apply {
            val privateKey = System.getenv("GPG_PRIVATE_KEY")
            val password = System.getenv("GPG_PASSWORD")
            isRequired = !hasProperty("dd-skip-signing")
            useInMemoryPgpKeys(privateKey, password)
            sign(publishingExtension.publications.getByName(MavenConfig.PUBLICATION))
        }
    }
}
