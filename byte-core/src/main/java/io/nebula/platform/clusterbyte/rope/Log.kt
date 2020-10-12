package io.nebula.platform.clusterbyte.rope

/**
 * level :
 * 1. log.e(): error info
 * 2. log.w(): warning info
 * 3. log.p(): information of plugin process, you can see the task or action's execution progress
 * 4. log.d(): detail of all task, it maybe great amount
 * 5. log.i(): some unnecessary log
 *
 * date : 2019-07-18 10:34
 * @author panxinghai
 */
class Log private constructor(private val defaultTag: String) {
    companion object {
        fun instance(defaultTag: String): Log {
            return Log(defaultTag)
        }
    }

    var level = 5

    fun e(tag: String, msg: String) {
        doErrorLog(1, tag, msg)
    }

    fun w(tag: String, msg: String) {
        doNormalLog(2, tag, msg)
    }

    fun p(tag: String, msg: String) {
        doNormalLog(3, tag, msg)
    }

    fun d(tag: String, msg: String) {
        doNormalLog(4, tag, msg)
    }

    fun i(msg: String) {
        doNormalLog(5, msg = msg)
    }

    fun e(msg: String) {
        doErrorLog(
            1,
            msg = "error: $msg"
        )
    }

    fun w(msg: String) {
        doNormalLog(
            2,
            msg = "warning: $msg"
        )
    }

    fun p(msg: String) {
        doNormalLog(3, msg = msg)
    }

    fun d(msg: String) {
        doNormalLog(4, msg = msg)
    }

    fun i(tag: String = defaultTag, msg: String) {
        doNormalLog(5, tag, msg)
    }

    fun test(msg: String) {
        doNormalLog(1, "test", msg)
    }

    private fun doNormalLog(level: Int, tag: String = defaultTag, msg: String) {
        if (level <= this.level) {
            println("$tag: $msg")
        }
    }

    private fun doErrorLog(level: Int, tag: String = defaultTag, msg: String) {
        if (level <= this.level) {
            System.err.println("$tag $msg")
        }
    }

}