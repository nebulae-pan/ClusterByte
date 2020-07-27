package io.nebula.plugin.platform.upload

/**
 * @author xinghai.pan
 *
 * date : 2020-07-27 15:57
 */
class PomExtension {
    var groupId: String = ""
    var artifactId: String = ""
    var version: String = ""

    fun groupId(id: String) {
        groupId = id
    }

    fun artifactId(id: String) {
        artifactId = id
    }

    fun version(version: String) {
        this.version = version
    }
}