import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    signing
}

val pagingPublishGroup = providers
    .gradleProperty("pagingPublishGroup")
    .orElse("ru.dapadz.paging")

val pagingPublishVersion = providers
    .gradleProperty("pagingPublishVersion")
    .orElse("1.0.0")

val pagingPomName = providers
    .gradleProperty("pagingPomName")
    .orElse("Paging")

val pagingPomDescription = providers
    .gradleProperty("pagingPomDescription")
    .orElse("Android pagination toolkit for RecyclerView-based UIs")

val pagingPomUrl = providers
    .gradleProperty("pagingPomUrl")
    .orElse("https://github.com/dapadz/paging")

val pagingPomScmConnection = providers
    .gradleProperty("pagingPomScmConnection")
    .orElse("scm:git:https://github.com/dapadz/paging.git")

val pagingPomScmDeveloperConnection = providers
    .gradleProperty("pagingPomScmDeveloperConnection")
    .orElse("scm:git:ssh://git@github.com/dapadz/paging.git")

val pagingDeveloperId = providers
    .gradleProperty("pagingDeveloperId")
    .orElse("dapadz")

val pagingDeveloperName = providers
    .gradleProperty("pagingDeveloperName")
    .orElse("dapadz")

val pagingDeveloperEmail = providers
    .gradleProperty("pagingDeveloperEmail")
    .orElse("dapadz@vk.com")

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val signingKeyFile = localProperties.getProperty("signingKeyFile")
val signingPassword = localProperties.getProperty("signingPassword")

group = pagingPublishGroup.get()
version = pagingPublishVersion.get()

android {
    namespace = "ru.dapadz.paging"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.recyclerview)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            val publication = (findByName("maven") as? MavenPublication)
                ?: create<MavenPublication>("maven") {
                    from(components["release"])
                }

            publication.artifactId = project.name
            publication.pom {
                name.set(pagingPomName.get())
                description.set(pagingPomDescription.get())
                url.set(pagingPomUrl.get())

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    url.set(pagingPomUrl.get())
                    connection.set(pagingPomScmConnection.get())
                    developerConnection.set(pagingPomScmDeveloperConnection.get())
                }

                developers {
                    developer {
                        id.set(pagingDeveloperId.get())
                        name.set(pagingDeveloperName.get())
                        email.set(pagingDeveloperEmail.get())
                    }
                }
            }
        }
    }
}

afterEvaluate {
    if (signingKeyFile.isNullOrBlank() || signingPassword.isNullOrBlank()) {
        logger.lifecycle("Signing is skipped for ${project.path}: set signingKeyFile and signingPassword in local.properties")
        return@afterEvaluate
    }

    val keyText = file(signingKeyFile).readText(Charsets.UTF_8)
    extensions.configure<SigningExtension>("signing") {
        useInMemoryPgpKeys(keyText, signingPassword)
        sign(extensions.getByType(PublishingExtension::class.java).publications["maven"])
    }
}

tasks.register("printSigningProps") {
    group = "publishing"
    description = "Prints whether signing properties are configured"

    doLast {
        println("signingKeyFile present: ${!signingKeyFile.isNullOrBlank()}")
        println("signingPassword present: ${!signingPassword.isNullOrBlank()}")
        if (!signingKeyFile.isNullOrBlank()) {
            println("signingKeyFile: $signingKeyFile")
        }
    }
}
