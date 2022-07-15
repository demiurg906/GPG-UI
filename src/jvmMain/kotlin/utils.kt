import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Result<out T : Any> {
    class Success<out T : Any>(val value: T) : Result<T>()

    sealed class Error : Result<Nothing>() {
        data class FromProcess(val stderr: String, val exitCode: Int) : Error()
        data class FromException(val e: Exception) : Error()
    }
}

fun <T : Any> T.success(): Result.Success<T> = Result.Success(this)

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> Result<T>.unwrap(onError: (Result.Error) -> T? = { null }): T {
    contract {
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> value
        is Result.Error -> {
            onError(this)?.let { return it }
            throw IllegalStateException(this.toString())
        }
    }
}

val Result<String>.stringValue: String
    get() = when (this) {
        is Result.Success -> value
        is Result.Error.FromProcess -> "Error: ${stderr}"
        is Result.Error.FromException -> "Exception: ${e.message}"
    }

data class KeyInfo(
    val pubInfo: String,
    val pubHash: String,
    val uid: String,
    val sub: String
) {
    companion object {
        private val UID_REGEX = """uid *\[(.*)] (.*?)( <(.*)>)?""".toRegex()
    }

    val name: String
    val trustLevel: String
    val email: String

    init {
        val groups = UID_REGEX.matchEntire(uid)?.groupValues ?: emptyList()
        name = groups.getOrElse(2) { "" }.trim()
        trustLevel = groups.getOrElse(1) { "" }.trim()
        email = groups.getOrElse(4) { "" }.trim()
    }

    val isValid: Boolean
        get() = name.isNotBlank()


    override fun toString(): String {
        return """
            $pubInfo
                  $pubHash
            $uid
            $sub
        """.trimIndent()
    }
}
