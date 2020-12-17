package io.nebula.plugin.platform.upload

import com.android.build.gradle.internal.tasks.factory.registerTask
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
        val extension = project.extensions.create("upload", UploadExtension::class.java, project)

        val uploadTask = project.tasks.getByName("uploadArchives") as Upload
        val repoHandler = uploadTask.repositories as HasConvention
        val convention =
            repoHandler.convention.plugins["maven"] as DefaultMavenRepositoryHandlerConvention

        val publishTasks = arrayListOf<PublishTask>()
        project.afterEvaluate {
            extension.repositories.forEach {
                val task = project.tasks.registerTask(
                    PublishTask.Action(
                        it.name.toLowerCase().capitalize()
                    )
                )
                val realTask = task.get()
                realTask.dependsOn(uploadTask)
                publishTasks.add(realTask)
            }
        }

        project.gradle.taskGraph.whenReady {
            publishTasks.forEach {
                if (!project.gradle.taskGraph.allTasks.contains(it)) {
                    return@forEach
                }
                val repo = extension.repositories.findByName(it.repoName.toLowerCase())
                    ?: return@whenReady
                convention.mavenDeployer { deployer ->
                    val remote = MavenRemoteRepository()
                    val url = project.uri(repo.url).toString()
                    remote.url = url
                    println("depolyer to: $url")
                    deployer.name = "mavenDeployer"
                    deployer.repository = remote
                    deployer.pom.apply {
                        groupId = extension.pom.groupId
                        artifactId = extension.pom.artifactId
                        version = extension.pom.version
                    }
                }
            }
        }
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