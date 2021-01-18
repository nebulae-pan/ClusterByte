package io.nebula.plugin.platform.upload

import org.gradle.api.Action

/**
 * Created by nebula on 2020-11-22
 */
abstract class Repository {
    abstract var name: String
    abstract var url: String

    val pom: PomExtension = PomExtension()

    fun pom(action: Action<PomExtension>) {
        action.execute(pom)
    }
}