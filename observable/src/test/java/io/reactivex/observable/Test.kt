package io.reactivex.observable

import io.reactivex.common.functions.Action

fun test() {
    Completable.complete().doFinally(object : Action {
        override fun invoke() {

        }
    })
}