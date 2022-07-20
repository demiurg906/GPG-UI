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



fun main() {
    val ecrypted = """
-----BEGIN PGP MESSAGE-----

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
IaZydDBBIxpnKJj9
=Zhnj
-----END PGP MESSAGE-----
    """.trimIndent()

//    val decrypted = GpgLauncher.decrypt(ecrypted).unwrap {  }
//    println(decrypted)

    val keys = GpgLauncher.getKeys().unwrap()

    val encrypted = GpgLauncher.encrypt(keys[0], keys[0], "some message").unwrap()

    println(encrypted)

    val decrypted = GpgLauncher.decrypt(encrypted).unwrap()

    println(decrypted)

}
