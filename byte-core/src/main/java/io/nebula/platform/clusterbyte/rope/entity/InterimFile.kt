package io.nebula.platform.clusterbyte.rope.entity

import com.android.build.api.transform.Status
import java.io.File

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 19:32
 */
abstract class InterimFile(
    private val file: File,
    private val outputDir: File,
    private val status: Status = Status.ADDED
) {

}