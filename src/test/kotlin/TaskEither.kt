import arrow.core.Either
import arrow.core.flatMap
import shared.TaskEither
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

//sealed class TaskEither<E, A> {
//
//    data class Left<T>(val mono: Mono<T>) : TaskEither<T, Nothing>()
//
//    data class Right<T>(val mono: Mono<T>) : TaskEither<Nothing, T>()
//
//
//    private fun <E, T> coerce(): TaskEither<E, T> = this as TaskEither<E, T>
//    private fun <T> coerceRight(): TaskEither<E, T> = coerce()
//    private fun <T> coerceLeft(): TaskEither<T, A> = coerce()
//
//    fun _mono(): Mono<A> = when (this) {
//        is Left -> this.coerceRight<A>()._mono()
//        is Right -> this.mono
//    }
//
//    fun <B> map(ab: (A) -> B): TaskEither<E, B> =
//        when (this) {
//            is Left -> this.coerceRight()
//            is Right -> right(this.mono.map(ab)).coerceLeft()
//        }
//
//    fun <B> flatMap(ab: (A) -> TaskEither<E, B>): TaskEither<E, B> =
//        when (this) {
//            is Left -> this.coerce()
//            is Right -> right(this.mono.flatMap { ab(it)._mono() }).coerceLeft()
//        }
//
//    fun <B> fold(eb: (E) -> B, ab: (A) -> B): Mono<B> =
//        when (this) {
//            is Left -> this.mono.map(eb)
//            is Right -> this.mono.map(ab)
//        }
//
//    /**
//     * Returns the value from this [Either.Right] or the given argument if this is a [Either.Left].
//     *
//     * Example:
//     * ```
//     * Right(12).getOrElse(17) // Result: 12
//     * Left(12).getOrElse(17)  // Result: 17
//     * ```
//     */
//    fun getOrElse(default: A): Mono<A> = when (this) {
//        is Left -> Mono.just(default)
//        is Right -> this._mono()
//    }
//
////    fun <E2> mapLeft(fn: (E) -> E2): TaskEither<E2, A> =
////        TaskEither(
////            _mono.onErrorMap {
////                when (it) {
////                    is ErrorContainer -> ErrorContainer(fn(it.error as E) as Any)
////                    else -> it
////                }
////            }
////        )
//
//    companion object {
//        fun <T> right(value: Mono<T>): TaskEither<Nothing, T> = Right(value)
//        fun <T> right(value: T): TaskEither<Nothing, T> = Right(Mono.just(value))
//
//        fun <T> left(value: Mono<T>): TaskEither<T, Nothing> = Left(value)
//        fun <T> left(value: T): TaskEither<T, Nothing> = Left(Mono.just(value))
//
//        fun <T> just(value: T): TaskEither<Nothing, T> = right(value)
//
//        fun <T> error(value: T): TaskEither<T, Nothing> = left(value)
//
//
//    }
//}


sealed class DBError {}

fun doDbStuff(): Either<DBError, Unit> = TODO()

sealed class ServiceError {}

fun doServicetuff(): Either<ServiceError, Unit> = TODO()

sealed class ProgramError {
    data class DB(val error: DBError) : ProgramError()
    data class Service(val error: ServiceError) : ProgramError()
    object Blah : ProgramError()
    object Blah2 : ProgramError()
}

val program = doDbStuff().mapLeft(ProgramError::DB)
    .flatMap { doServicetuff().mapLeft(ProgramError::Service) }


sealed class TaskDBError {}

fun doTaskDbStuff(): TaskEither<TaskDBError, Unit> = TODO()

sealed class TaskServiceError {}

fun doTaskServicetuff(): TaskEither<TaskServiceError, Unit> = TODO()

sealed class TaskProgramError {
    data class DB(val error: TaskDBError) : TaskProgramError()
    data class Service(val error: TaskServiceError) : TaskProgramError()
    object Blah : TaskProgramError()
    object Blah2 : TaskProgramError()
}

//val programTask = doTaskDbStuff().mapLeft(TaskProgramError::DB)
//    .flatMap { doTaskServicetuff().mapLeft(TaskProgramError::Service) }



@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonoEitherTest {

    @Test
    fun `flatMap - maps value on Right`() {
        val either = Either.right(5).flatMap { Either.right("string") }
        val task: TaskEither<Nothing, String> = TaskEither.right(5).flatMap { int -> TaskEither.right(int.toString()) }

//        StepVerifier.create(task.fold(::identity, ::identity))
//            .expectNext("5")
//            .verifyComplete()
    }

    @Test
    fun `flatMap - flatMapping on a left does nothing`() {

        val either = Either.left(5).flatMap { Either.right("string") }
        val either2 = Either.right(5).flatMap { Either.left("string") }.flatMap { Either.left(3) }
//        val task2 = TaskEither.right(5).flatMap { TaskEither.left("string") }.flatMap { TaskEither.left(3) }

        val task: TaskEither<String, Int> =
            TaskEither.left("left").flatMap { TaskEither.right(5) }

//        StepVerifier.create(task.fold(::identity, Int::toString))
//            .expectNext("left")
//            .verifyComplete()
    }

//    @Test
//    fun `Blah is ind`() {
//        val either = Either.right("right").flatMap { Either.left("left") }
//        val task =
//            TaskEither.right("right").flatMap { }
//        <
//
//        StepVerifier.create(task.fold(::identity, Int::toString))
//            .expectNext("left")
//            .verifyComplete()
//    }

    // returned flatmap is a left


//    @Test
//    fun `map() - Transforms value`() {
//        val right = TaskEither.right(5).map(::times2)
//
//        StepVerifier.create(right.mono)
//            .expectNext(10)
//            .verifyComplete()
//    }
//
//    @Test
//    fun `map() - Shortcircuits on MonoError`() {
//        val spyMapFn = mock<(Any) -> Unit>()
//        val leftValue = Error("Failed")
//        val left = TaskEither.left(leftValue).map(spyMapFn)
//
//        StepVerifier.create(left.mono)
//            .verifyErrorMatches { it == leftValue }
//
//        verify(spyMapFn, times(0)).invoke(any())
//    }
//
//    @Test
//    fun `flatMap() - Transforms Error type`() {
//
////        can I leftMap on type if i hide it inside an internal error and not require a throwable
//
//        val te = TaskEither.error("")
//        Either.Left
//        when (te) {
//            is TaskEither.Left -> TODO()
//        }
//
//        val left = TaskEither.left<ProgramError>(ProgramError.Blah2).mapLeft { err ->
//            when (err) {
//                is ProgramError.Blah -> println("Yeaaaah")
//                is ProgramError.Blah2 -> println("Yeaaaah2")
//                else -> println("nooooo")
//            }
//        }
//
//        left.mono.block()
//
//
//    }

    private fun <T> identity(value: T): T = value

    private fun times2(x: Int): Int = x * 2
}
