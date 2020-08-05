package io.nebula.platform.cluster.plugin

import com.android.build.gradle.BaseExtension
import io.nebula.platform.clusterbyte.converter.asm.AsmClassNodeConverterFactory
import io.nebula.platform.clusterbyte.converter.asm.AsmConverterFactory
import io.nebula.platform.clusterbyte.core.ClusterExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by nebula on 2020-04-05
 */
class ClusterPlugin : Plugin<Project> {
    override fun apply(p: Project) {
        val androidExtension = p.extensions.findByType(BaseExtension::class.java)
        val clusterExtension = p.extensions.create("cluster", ClusterExtension::class.java)
        androidExtension?.registerTransform(ClusterTransform(p, clusterExtension))

        clusterExtension.registerClassConverterFactory(AsmConverterFactory())
        clusterExtension.registerClassConverterFactory(AsmClassNodeConverterFactory())
    }
}