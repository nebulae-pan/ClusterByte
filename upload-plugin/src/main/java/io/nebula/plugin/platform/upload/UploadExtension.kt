package io.nebula.plugin.platform.upload

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

/**
 * @author xinghai.pan
 *
 * date : 2020-07-24 16:18
 */
open class UploadExtension(project: Project) {
    private val container =
        project.container(Repository::class.java) {
            val repo = project.objects.newInstance(Repository::class.java)
            repo.name = it
            repo
        }

    fun repositories(action: Action<NamedDomainObjectContainer<Repository>>) {
        action.execute(container)
    }
}