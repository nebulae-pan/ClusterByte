package io.nebula.plugin.platform.upload

import com.android.build.gradle.internal.tasks.factory.registerTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
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
                val deployer = mavenDeployer
                val remote = MavenRemoteRepository()
                val url = project.uri(repo.url).toString()
                remote.url = url
                println("deploy to: $url")
                deployer.repository = remote
                deployer.pom.whenConfigured { pom ->
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
                deployer.pom.apply {
                    groupId = extension.pom.groupId
                    artifactId = extension.pom.artifactId
                    version = extension.pom.version
                }
            }
        }
    }

    private fun applySubstitutions(
        scopeMappings: Conf2ScopeMappingContainer,
        extension: UploadExtension,
        project: Project
    ) {
        if (scopeMappings.mappings.isEmpty()) {
            configureJavaScopeMappings(project.configurations, scopeMappings)
        }
        scopeMappings.apply {
            val replacedConfiguration = project.configurations.create("replaced$")
            val notationList = arrayListOf<Any>()
            project.configurations.all {
                if (it.name == "replaced") {
                    return@all
                }
                val conf2scopeMapping = mappings[it]
                    ?: return@all
                val configuration = conf2scopeMapping.configuration
                configuration.allDependencies.forEach { dep ->
                    if (dep is ProjectDependency) {
                        val moduleName = extension.substitutions[dep.dependencyProject.name]
                            ?: return@forEach
                        notationList.add(moduleName)
                    } else {
                        notationList.add(dep)
                    }
                }
                notationList.forEach { notation ->
                    project.dependencies.add("replaced", notation)
                }
                mappings.remove(it)
                val scope = if (it.name == "implementation") {
                    "runtime"
                } else {
                    "compile"
                }
                addMapping(conf2scopeMapping.priority, replacedConfiguration, scope)
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

    private fun configureJavaScopeMappings(
        configurations: ConfigurationContainer,
        mavenScopeMappings: Conf2ScopeMappingContainer
    ) {
        mavenScopeMappings.addMapping(
            MavenPlugin.COMPILE_PRIORITY,
            configurations.getByName("api"),
            Conf2ScopeMappingContainer.COMPILE
        )
        mavenScopeMappings.addMapping(
            MavenPlugin.RUNTIME_PRIORITY,
            configurations.getByName("implementation"),
            Conf2ScopeMappingContainer.RUNTIME
        )
        mavenScopeMappings.addMapping(
            MavenPlugin.RUNTIME_PRIORITY,
            configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME),
            Conf2ScopeMappingContainer.RUNTIME
        )
    }
}