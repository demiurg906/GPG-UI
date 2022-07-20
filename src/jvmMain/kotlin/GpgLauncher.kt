import java.io.File
import java.io.InputStream
import javax.xml.crypto.dsig.keyinfo.PGPData
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object GpgLauncher {
    fun addNewKey(key: String): Result<String> {
        return withTempFile("publicKey") { file ->
            file.writeText(key)
            runGpg("--import", file.absolutePath)
        }
    }

    fun decrypt(input: String): Result<String> {
        return withTempFile("encrypted") { file ->
            file.writeText(input)
            runGpg("-d", file.absolutePath)
        }
    }

    fun encrypt(sender: KeyInfo, recipient: KeyInfo, input: String): Result<String> {
        return withTempFile("decrypted") { file ->
            file.writeText(input)
            runGpg("-e", "-u", sender.name, "-r", recipient.name, "-r", sender.name, "--trust-model", "always", "--armor", "--output", "-", file.absolutePath)
        }
    }

    fun sign(input: String): Result<String> {
        return withTempFile("signed") { file ->
            file.writeText(input)
            runGpg("--clearsign", "-o", "-", file.absolutePath)
        }
    }

    fun getKeys(): Result<List<KeyInfo>> {
        val output = runGpg("--list-public-keys").unwrap { return it }
        val iterator = output.lines().filter { it.isNotBlank() }.iterator()
        return buildList {
            while (iterator.hasNext()) {
                if (iterator.next() == "--------------------------------") break
            }
            while (iterator.hasNext()) {

                val info = KeyInfo(
                    iterator.next().trim(),
                    iterator.next().trim(),
                    iterator.next().trim(),
                    iterator.next().trim(),
                )
                add(info)
            }
        }.success()
    }

    private fun runGpg(vararg arguments: String): Result<String> {
        println(arguments.joinToString(" ", prefix = "gpg "))
        val process = ProcessBuilder("gpg", *arguments).start()
        val exitCode = process.waitFor()

        return if (exitCode == 0) {
            Result.Success(process.inputStream.readOutput())
        } else {
            Result.Error.FromProcess(process.errorStream.readOutput(), exitCode)
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <R> withTempFile(prefix: String, block: (File) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val file = File.createTempFile(prefix, ".txt")
        return try {
            block(file)
        } finally {
            file.delete()
        }
    }

    private fun InputStream.readOutput(): String = bufferedReader().use { it.readLines() }.joinToString("\n")
}

