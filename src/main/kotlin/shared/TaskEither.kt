package shared

import reactor.core.publisher.Mono

sealed class TaskEither<out E, out A> {
    infix fun <B> map(ab: (A) -> B) : TaskEither<E, B> = TODO()

    infix fun <B> flatMap(ab: (A) -> TaskEither<@UnsafeVariance E, B>): TaskEither<E, B> = TODO()

    infix fun <EB> mapLeft(eb: (E) -> EB) : TaskEither<EB, A> = TODO()

    infix fun <EB, B> flatMapLeft(fn: (E) -> TaskEither<EB, B>): TaskEither<EB, B> = TODO()

    infix fun shortcircuit<EB>(e:  EB): TaskEither<EB, Nothing> = TODO()

    data class Left<out E> internal constructor(val a: Mono<@UnsafeVariance E>) : TaskEither<E, Nothing>()

    data class Right<out A> internal constructor(val b: Mono<@UnsafeVariance A>) : TaskEither<Nothing, A>()

    companion object {
        fun <L> left(value: Mono<L>): TaskEither<L, Nothing> = Left(value)
        fun <L> left(value: L): TaskEither<L, Nothing> = left(value)

        fun <R> right(value: Mono<R>): TaskEither<Nothing, R> = Right(value)
        fun <R> right(value: R): TaskEither<Nothing, R> = right(value)

        fun <A> fromNullable(a: A?): TaskEither<Unit, A> = if (a != null) right(a) else left(Unit)

        fun <A>fromMono(mono: Mono<A>): TaskEither<Throwable, A> = right(mono)
    }
}

fun <T> Mono<T>.task(): TaskEither<Throwable, T> = TaskEither.right(this)

fun <A> A.right(): TaskEither<Nothing, A> = TaskEither.right(this)
fun <A> A.left(): TaskEither<A, Nothing> = TaskEither.left(this)
