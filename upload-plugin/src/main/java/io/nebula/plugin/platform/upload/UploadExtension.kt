package io.nebula.plugin.platform.upload

import org.gradle.api.Action
import org.gradle.api.artifacts.maven.MavenPom

/**
 * @author xinghai.pan
 *
 * date : 2020-07-24 16:18
 */
open class UploadExtension {
    var local: Boolean = true
    var localRepo: String = ""
    var remoteRepo: String = ""

    internal var pom: MavenPom? = null

    fun pom(action: Action<MavenPom>) {
        val pom = this.pom ?: return
        action.execute(pom)
    }
}