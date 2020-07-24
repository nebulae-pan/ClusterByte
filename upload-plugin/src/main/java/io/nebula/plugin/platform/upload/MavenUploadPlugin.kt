package io.nebula.plugin.platform.upload

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator
import org.gradle.api.internal.artifacts.mvnsettings.MavenSettingsProvider
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.MavenPluginConvention
import org.gradle.api.publication.maven.internal.DefaultDeployerFactory
import org.gradle.api.publication.maven.internal.MavenFactory
import org.gradle.api.publication.maven.internal.deployer.MavenRemoteRepository
import org.gradle.api.tasks.Upload
import org.gradle.internal.Factory
import org.gradle.internal.logging.LoggingManagerInternal
import java.util.*
import javax.inject.Inject

class MavenUploadPlugin @Inject constructor(
    private val loggingManagerFactory: Factory<LoggingManagerInternal>,
    private val fileResolver: FileResolver,
    private val mavenSettingsProvider: MavenSettingsProvider,
    private val mavenRepositoryLocator: LocalMavenRepositoryLocator
) : Plugin<ProjectInternal> {

    override fun apply(project: ProjectInternal) {
        project.pluginManager.apply("maven")
        loadLocalProperties(project)
        val extension = project.extensions.create("upload", UploadExtension::class.java)

        if (!isUploadTask(project)) {
            return
        }
        val task = project.tasks.getByName("uploadArchives") as Upload

        config(project, task, extension)
    }

    private fun config(
        project: ProjectInternal,
        task: Upload,
        extension: UploadExtension
    ) {
        val mavenFactory =
            project.services.get(MavenFactory::class.java)
        val mavenConvention = MavenPluginConvention(project, mavenFactory)
        val convention = project.convention
        convention.plugins["maven"] = mavenConvention
        val deployerFactory = DefaultDeployerFactory(
            mavenFactory,
            loggingManagerFactory,
            fileResolver,
            mavenConvention,
            project.configurations,
            mavenConvention.conf2ScopeMappings,
            mavenSettingsProvider,
            mavenRepositoryLocator
        )
        val pomFactory = mavenFactory.createMavenPomFactory(
            project.configurations,
            mavenConvention.conf2ScopeMappings,
            fileResolver
        )
        pomFactory.create().apply {
            extension.pom = this
        }
        project.afterEvaluate {
            val url = if (extension.local) extension.localRepo else extension.remoteRepo
            if (url.isEmpty()) {
                throw IllegalArgumentException("upload.remoteRepo is not config.")
            }

            if (!extension.local) {
                println("INFO:upload ${project.name} to remote:${project.uri(url)}? Y/N ")
                println("input:")
                val input = readLine()
                if (input?.toLowerCase(Locale.getDefault()) != "y") {
                    throw RuntimeException("user terminate gradle build.")
                }
            }
            deployerFactory.createMavenDeployer().apply {
                val remote = MavenRemoteRepository()
                remote.url = project.uri(url).toString()
                name = "mavenDeployer"
                repository = remote
                pom = extension.pom

                task.repositories.add(this)
            }
        }
    }

    private fun isUploadTask(project: ProjectInternal): Boolean {
        project.gradle.startParameter.taskNames.forEach {
            if (it.contains("uploadArchives")) {
                return true
            }
        }
        return false
    }

    private fun loadLocalProperties(project: Project) {
        val file = project.rootProject.file("local.properties")
        if (file.exists()) {
            val inputStream = file.inputStream()
            val properties = Properties()
            properties.load(inputStream)
            val ext = project.extensions.getByName("ext") as ExtraPropertiesExtension
            properties.forEach {
                ext.set(it.key.toString(), it.value)
            }
        }
    }
}