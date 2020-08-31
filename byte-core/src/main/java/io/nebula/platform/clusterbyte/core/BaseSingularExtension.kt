package io.nebula.platform.clusterbyte.core

import org.gradle.api.Project

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 14:50
 */
abstract class BaseSingularExtension {
    abstract var closureName: String

    protected lateinit var project: Project

    fun setParams(project: Project) {
        this.project = project
    }
}