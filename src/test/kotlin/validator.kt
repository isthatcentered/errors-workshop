import Validated.Invalid
import Validated.Valid
import Validation.Companion.forValue
import org.junit.jupiter.api.TestInstance
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just
import reactor.test.StepVerifier
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


sealed class Validated<T> {
	data class Valid<T>(val value: T) : Validated<T>()
	data class Invalid<T>(val reason: String) : Validated<T>()
}

private typealias asyncCheck<T> = (Value: T) -> Mono<Validated<T>>

private typealias syncCheck<T> = (value: T) -> String?

private typealias guardFn<T> = (value: T) -> Boolean


sealed class Validation<T> {
	// CONSTRUCTORS
	data class Ok<T>(val value: T) : Validation<T>()

	data class KO<T>(val reason: String) : Validation<T>()

	data class Custom<T>(val fn: asyncCheck<T>) : Validation<T>()

	data class Equals<T>(val expected: T, val error: String) : Validation<T>()

	// OPERATORS
	data class Conditional<T>(val shouldRun: (value: T) -> Boolean, val validator: Validation<T>) : Validation<T>()

	data class Sequence<T>(val left: Validation<T>, val right: Validation<T>) : Validation<T>()

	data class Nested<T, V>(val prop: KProperty1<T, V>, val validation: Validation<V>) : Validation<T>()

	// METHODS
	infix fun then(other: Validation<T>): Validation<T> = Sequence(this, other)

	infix fun onlyWhen(guard: guardFn<T>): Validation<T> = Conditional(guard, this)

	companion object {
		fun <T> ok(): Validation<T> = sync { null }

		fun <T> sync(fn: syncCheck<T>): Validation<T> = Custom(liftSync(fn))

		fun <T> async(fn: asyncCheck<T>): Validation<T> = Custom(fn)

		fun <T, V> propEquals(key: KProperty1<T, V>, error: String): (T) -> Validation<T> = { expected: T -> Nested(key, Equals(expected, error)) }

		fun <T> forValue(validator: Validation<T>, value: T): Mono<Validated<T>> = when (validator) {
			is Ok -> just(Valid(value))
			is KO -> just(Invalid(validator.reason))
			is Custom ->
				validator.fn(value)
			is Equals ->
				forValue(Custom(liftSync { if (value === validator.expected) null else validator.error }), value)
			is Nested<T, *> ->
				forValue(validator.validation as Validation<Any>, validator.prop.get(value) as Any)
					.flatMap {
						when (it) {
							is Valid -> forValue(Ok(value), value)
							is Invalid -> forValue(KO(it.reason), value)
						}
					}
			is Conditional ->
				if (validator.shouldRun(value))
					forValue(validator.validator, value)
				else forValue(Ok(value), value)

			is Sequence ->
				forValue(validator.left, value)
					.flatMap {
						when (it) {
							is Valid -> forValue(validator.right, value)
							is Invalid -> forValue(KO(it.reason), value)
						}
					}
		}

		private fun <T> liftSync(validation: syncCheck<T>): asyncCheck<T> {
			return { value ->
				when (val result = validation(value)) {
					null -> just(Valid(value))
					else -> just(Invalid(result))
				}
			}
		}
	}
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Blah {

	@Test
	fun `Validation failure returns an Invalid(reason)`() {
		val alwaysFailingValidatorError = "always_failing_validator_error"
		val alwaysFailingValidator = alwaysFailingValidator<String>(alwaysFailingValidatorError)

		StepVerifier.create(forValue(alwaysFailingValidator, "value"))
			.expectNext(Invalid(alwaysFailingValidatorError))
			.verifyComplete()
	}

	@Test
	fun `Validation success returns a Valid(T)`() {
		val alwaysSucceedsValidator = alwaysPassingValidator<String>()
		val value = "value"

		StepVerifier.create(forValue(alwaysSucceedsValidator, value))
			.expectNext(Valid(value))
			.verifyComplete()
	}

	@Test
	fun `Sequence - Short circuits on failure`() {
		val firstValidatorErrorMessage = "first_validator_error_message"
		val firstAndFailingValidator = alwaysFailingValidator<String>(firstValidatorErrorMessage)
		val secondValidatorThatShouldNotBeTriggered = failsTestIfTriggeredValidator()

		val sequence = firstAndFailingValidator then secondValidatorThatShouldNotBeTriggered

		StepVerifier.create(forValue(sequence, "value"))
			.expectNext(Invalid(firstValidatorErrorMessage))
			.verifyComplete()
	}

	@Test
	fun `Sequence - Executes all validators until failure`() {
		val thirdValidatorErrorMessage = "third_validator_error_message"
		val firstAndPassingValidator = alwaysPassingValidator<String>()
		val secondAndPassingValidator = alwaysPassingValidator<String>()
		val thirdAndFailingValidator = alwaysFailingValidator<String>(thirdValidatorErrorMessage)

		val sequence = firstAndPassingValidator then secondAndPassingValidator then thirdAndFailingValidator

		StepVerifier.create(forValue(sequence, "value"))
			.expectNext(Invalid(thirdValidatorErrorMessage))
			.verifyComplete()
	}

	@Test
	fun `Guard - Failing guard is a pass through`() {
		val shouldNotBeTriggeredValidator = failsTestIfTriggeredValidator()
		val failingGate = { _: String -> false }

		val guardedValidator = shouldNotBeTriggeredValidator onlyWhen failingGate

		StepVerifier.create(forValue(guardedValidator, "value"))
			.expectNext(Valid("value"))
			.verifyComplete()
	}

	@Test
	fun `Guard - Execute validator if condition succeeds`() {
		val alwaysFailingValidatorMessage = "always_failing_validator_message"
		val alwaysFailingValidator = alwaysFailingValidator<String>(alwaysFailingValidatorMessage)
		val succeedingGate = { _: String -> true }

		val guardedValidator = alwaysFailingValidator onlyWhen succeedingGate

		StepVerifier.create(forValue(guardedValidator, "value"))
			.expectNext(Invalid(alwaysFailingValidatorMessage))
			.verifyComplete()
	}

	@Test
	fun `Equals - array`() {
		data class TestClass(val id: String)

		val actual = TestClass("123")
		val expected = TestClass("321")

		val validationErrorMessage = "validation_error_message"
		val idsAreEqualValidator = Validation.propEquals(TestClass::id, validationErrorMessage)

		assertEquals(Invalid(validationErrorMessage), forValue(idsAreEqualValidator(expected), actual).block())
	}

	@Test
	fun canInferKeys() {
		// https://kotlinlang.org/docs/tutorials/kotlin-for-py/member-references-and-reflection.html
		fun <T> prop(key: KProperty1<T, *>) = key::get

		data class Indeed(val id: String)

		val result = prop(Indeed::id)(Indeed("1234"))

		assertEquals("1234", result)


//		run(Validated.propEquals(prop,error)(1234) flatMap Validated.propNotNull(prop, error))
//			.map(validated =>
//
//			)

	}


	@Test
	fun `Equality in kotlin`() {

		assertEquals(true, "a" === "a", "a === a")
		assertEquals(true, "a" === "b", "a === b")

		assertEquals(true, "a" == "a", "a == a")
		assertEquals(true, "a" == "b", "a == b")

		fun indeed(hello: String) = 4

		fun <T, R> blah(callable: KFunction1<T, R>): KFunction1<T, Mono<R>> {

			fun enhanced(param: T) = Mono.fromCallable { callable.call(param) }

			return ::enhanced
		}


		val c = blah(::indeed)

	}

	private fun failsTestIfTriggeredValidator(): Validation<String> = Validation.sync { fail("This validator should not have been triggered") }

	private fun <T> alwaysFailingValidator(reason: String): Validation<T> = Validation.KO(reason)

	private fun <T> alwaysPassingValidator(): Validation<T> = Validation.ok()
}
