package io.nebula.plugin.platform.upload

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publication.maven.internal.DefaultMavenRepositoryHandlerConvention
import org.gradle.api.publication.maven.internal.deployer.MavenRemoteRepository
import org.gradle.api.tasks.Upload
import java.util.*

class MavenUploadPlugin : Plugin<ProjectInternal> {

    override fun apply(project: ProjectInternal) {
        project.pluginManager.apply("maven")
        loadLocalProperties(project)
        val extension = project.extensions.create("upload", UploadExtension::class.java)

        if (!isUploadTask(project)) {
            return
        }
        val task = project.tasks.getByName("uploadArchives") as Upload
        val repoHandler = task.repositories as HasConvention
        val convention =
            repoHandler.convention.plugins["maven"] as DefaultMavenRepositoryHandlerConvention

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
            convention.mavenDeployer {
                val remote = MavenRemoteRepository()
                remote.url = project.uri(url).toString()
                it.name = "mavenDeployer"
                it.repository = remote
                it.pom.apply {
                    groupId = extension.pom.groupId
                    artifactId = extension.pom.artifactId
                    version = extension.pom.version
                }
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