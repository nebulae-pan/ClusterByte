package io.nebula.plugin.platform.upload

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * @author xinghai.pan
 *
 * date : 2020-07-24 16:18
 */
open class UploadExtension(project: Project) {
    val pom: PomExtension = PomExtension()

    val repositories: NamedDomainObjectContainer<Repository> =
        project.container(Repository::class.java) {
            val repo = project.objects.newInstance(Repository::class.java)
            repo.name = it
            repo
        }

    val substitutions = hashMapOf<String, String>()

    fun pom(action: Action<PomExtension>) {
        val pom = this.pom
        action.execute(pom)
    }

    fun repositories(action: Action<NamedDomainObjectContainer<Repository>>) {
        action.execute(repositories)
    }

    fun substitute(projectName: String, moduleName: String) {
        substitutions[projectName] = moduleName
    }
}