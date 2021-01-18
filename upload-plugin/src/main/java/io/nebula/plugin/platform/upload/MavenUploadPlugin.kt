package io.nebula.plugin.platform.upload

import com.android.build.gradle.internal.tasks.factory.registerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.GroovyMavenDeployer
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

        val uploadTask = project.tasks.getByName("uploadArchives") as Upload
        val repoHandler = uploadTask.repositories as HasConvention
        val taskConvention =
            repoHandler.convention.plugins["maven"] as DefaultMavenRepositoryHandlerConvention
        val extension = project.extensions.create(
            "upload", UploadExtension::class.java,
            project
        )
        val mavenDeployer = taskConvention.mavenDeployer()
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
                val remote = MavenRemoteRepository()
                val url = project.uri(repo.url).toString()
                remote.url = url
                println("deploy to: $url")
                mavenDeployer.repository = remote
                dependenciesSubstitute(mavenDeployer, extension)
                mavenDeployer.pom.apply {
                    val pomGroupId = if (repo.pom.groupId == "") {
                        extension.pom.groupId
                    } else {
                        repo.pom.groupId
                    }
                    if (pomGroupId != "") {
                        groupId = pomGroupId
                    }

                    val pomArtifactId = if (repo.pom.artifactId == "") {
                        extension.pom.artifactId
                    } else {
                        repo.pom.artifactId
                    }
                    if (pomArtifactId != "") {
                        artifactId = pomArtifactId
                    }

                    val pomVersion = if (repo.pom.version == "") {
                        extension.pom.version
                    } else {
                        repo.pom.version
                    }
                    if (pomVersion != "") {
                        version = pomVersion
                    }
                }
            }
        }
    }

    private fun dependenciesSubstitute(
        mavenDeployer: GroovyMavenDeployer,
        extension: UploadExtension
    ) {
        mavenDeployer.pom.whenConfigured { pom ->
            val newDependencies = arrayListOf<Any?>()
            pom.dependencies.forEach inner@{ dep ->
                if (dep == null || dep::class.java.name != "org.apache.maven.model.Dependency") {
                    return@inner
                }
                val versionField = dep.javaClass.getDeclaredField("version")
                versionField.isAccessible = true
                val version = versionField.get(dep)

                if (version == "unspecified") {
                    val artifactIdField = dep.javaClass.getDeclaredField("artifactId")
                    artifactIdField.isAccessible = true
                    val artifactId = artifactIdField.get(dep)
                    val moduleName = extension.substitutions[artifactId]
                        ?: return@whenConfigured
                    println("replace:$artifactId -> $moduleName")
                    val groupIdField = dep.javaClass.getDeclaredField("groupId")
                    groupIdField.isAccessible = true
                    val strings = moduleName.split(':')
                    groupIdField.set(dep, strings[0])
                    artifactIdField.set(dep, strings[1])
                    versionField.set(dep, strings[2])
                    newDependencies.add(dep)
                } else {
                    newDependencies.add(dep)
                }
            }
            pom.dependencies = newDependencies
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