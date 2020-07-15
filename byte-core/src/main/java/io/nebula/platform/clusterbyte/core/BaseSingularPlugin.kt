package io.nebula.platform.clusterbyte.core

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:50
 */
abstract class BaseSingularPlugin : Plugin<Project> {
    protected var androidExtension: BaseExtension? = null
    protected var clusterExtension: ClusterExtension? = null

    override fun apply(p: Project) {
        androidExtension = p.extensions.findByType(BaseExtension::class.java)
        clusterExtension = p.extensions.findByType(ClusterExtension::class.java)
        apply0(p)
    }

    abstract fun apply0(p: Project)

    abstract fun runAlone(): Boolean

    protected fun registerTransform(transform: BaseSingularTransform) {
        if (runAlone() || clusterExtension == null) {
            androidExtension?.registerTransform(transform)
            return
        }
        clusterExtension?.registerRegularTransform(transform)
    }
}