import org.gradle.api.publish.PublishingExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    `maven-publish`
}

val pagingPublishVersion = providers
    .gradleProperty("pagingPublishVersion")
    .orElse("1.0.0")

subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                maven {
                    name = "localBundle"
                    url = uri(rootProject.layout.buildDirectory.dir("local-maven"))
                }
            }
        }
    }
}

tasks.register("publishPaging") {
    group = "publishing"
    description = "Publishes the paging library"
    dependsOn(":paging:publish")
}

tasks.register("publishPagingToLocalBundle") {
    group = "publishing"
    description = "Publishes the paging library to root build/local-maven"
    dependsOn(":paging:publishMavenPublicationToLocalBundleRepository")
}

val stagePagingForCentral by tasks.registering(Sync::class) {
    group = "publishing"
    description = "Stages the paging Maven repository layout for a Central Portal bundle"
    dependsOn("publishPagingToLocalBundle")

    from(layout.buildDirectory.dir("local-maven"))
    into(layout.buildDirectory.dir("central-staging"))

    exclude("**/maven-metadata*.xml")
    exclude("**/maven-metadata*.xml.*")
}

tasks.register("bundlePagingForCentral") {
    group = "publishing"
    description = "Creates a Sonatype Central upload bundle zip from the staged paging artifacts"
    dependsOn(stagePagingForCentral)

    val stagingDir = layout.buildDirectory.dir("central-staging")
    val outputFile = layout.buildDirectory.file(
        "central-bundle/paging-${pagingPublishVersion.get()}-central-bundle.zip"
    )

    inputs.dir(stagingDir)
    outputs.file(outputFile)

    doLast {
        val staging = stagingDir.get().asFile
        val bundle = outputFile.get().asFile

        bundle.parentFile.mkdirs()
        if (bundle.exists()) {
            bundle.delete()
        }

        ant.withGroovyBuilder {
            "zip"(
                "destfile" to bundle,
                "basedir" to staging,
                "filesonly" to true,
            )
        }

        println("Central bundle created at: ${bundle.absolutePath}")
    }
}

tasks.register("printSigningProps") {
    group = "publishing"
    description = "Prints signing property availability for the paging library"
    dependsOn(":paging:printSigningProps")
}
