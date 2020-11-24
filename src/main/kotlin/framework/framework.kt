package framework

import reactor.core.publisher.Mono

data class ServerResponse<T>(val statusCode: Int, val data: T?)
data class RequestBody<T>(val data: T) {
	fun validate() {}
}

fun <T> respond(status: Int, data: T?): Mono<ServerResponse<T>> = Mono.just(ServerResponse(status, data))
