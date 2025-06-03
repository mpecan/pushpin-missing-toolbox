import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Common publishing configuration for all Pushpin Missing Toolbox modules.
 * This function configures the maven publishing settings for a module.
 */
fun Project.configurePushpinPublishing(
    moduleName: String,
    moduleDescription: String
) {
    // Configure Kotlin toolchain
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        jvmToolchain(17)
    }
    
    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
        
        configure(
            KotlinJvm(
                javadocJar = JavadocJar.Javadoc(),
                sourcesJar = true,
            ),
        )
        
        coordinates("io.github.mpecan", moduleName, version.toString())
        
        pom {
            name.set(moduleName)
            description.set(moduleDescription)
            inceptionYear.set("2025")
            url.set("https://github.com/mpecan/pushpin-missing-toolbox")
            
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("https://opensource.org/licenses/MIT")
                }
            }
            
            developers {
                developer {
                    id.set("mpecan")
                    name.set("Pushpin Missing Toolbox Team")
                    url.set("https://github.com/mpecan/pushpin-missing-toolbox")
                }
            }
            
            scm {
                url.set("https://github.com/mpecan/pushpin-missing-toolbox")
                connection.set("scm:git:git://github.com/mpecan/pushpin-missing-toolbox.git")
                developerConnection.set("scm:git:ssh://git@github.com/mpecan/pushpin-missing-toolbox.git")
            }
        }
    }
}
