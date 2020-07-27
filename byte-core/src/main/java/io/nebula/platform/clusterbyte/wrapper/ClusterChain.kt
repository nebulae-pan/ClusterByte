package io.nebula.platform.clusterbyte.wrapper

/**
 * Created by nebula on 2020-07-19
 */
class ClusterChain<T>(private var piece: T) {

    fun piece(): T {
        return this.piece
    }

    fun processed(piece: T) {
        this.piece = piece
    }
}