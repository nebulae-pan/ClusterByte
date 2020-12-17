package io.nebula.plugin.platform.upload

import com.android.build.gradle.internal.tasks.factory.TaskCreationAction
import org.gradle.api.DefaultTask

/**
 * @author xinghai.pan
 *
 * date : 2020-12-16 10:14
 */
open class PublishTask : DefaultTask() {
    var repoName = ""

    class Action(private val repoName: String) :
        TaskCreationAction<PublishTask>() {
        override val name: String
            get() = "publishTo$repoName"
        override val type: Class<PublishTask>
            get() = PublishTask::class.java

        override fun configure(task: PublishTask) {
            task.repoName = repoName
        }
    }
}