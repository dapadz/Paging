import org.gradle.api.publish.PublishingExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    `maven-publish`
}

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

tasks.register("printSigningProps") {
    group = "publishing"
    description = "Prints signing property availability for the paging library"
    dependsOn(":paging:printSigningProps")
}
