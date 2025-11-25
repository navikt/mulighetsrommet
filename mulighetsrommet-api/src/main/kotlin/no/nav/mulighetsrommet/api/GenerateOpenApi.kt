package no.nav.mulighetsrommet.api

import io.github.smiley4.ktoropenapi.OpenApiPlugin
import io.ktor.server.application.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    if (args.isEmpty() || args.size % 2 != 0) {
        println("Usage: OpenApiGenerator <specName1> <outputFile1> [<specName2> <outputFile2> ...]")
        exitProcess(1)
    }

    val specs = args.toList().chunked(2).map { (openApiSpecName, fileOutputPath) ->
        openApiSpecName to fileOutputPath
    }

    val server = createServer(ApplicationConfigLocal)

    server.application.monitor.subscribe(ServerReady) {
        println("✅ Server is ready")

        try {
            specs.forEach { (openApiSpecName, fileOutputPath) ->
                generateOpenApiSpec(openApiSpecName, fileOutputPath)
            }
        } catch (e: Exception) {
            System.err.println("   -> ❌ Failed to generate specs: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        } finally {
            println("✅ Shutting down Ktor server...")
            server.stop(1.seconds.inWholeMilliseconds, 5.seconds.inWholeMilliseconds)

            println("✅ Server stopped. Generator finished.")
            exitProcess(0)
        }
    }

    server.start(wait = false)
}

private fun generateOpenApiSpec(openApiSpecName: String, outputPath: String) {
    val spec = OpenApiPlugin.getOpenApiSpec(openApiSpecName)
    val hash = sha256Of(spec)

    val specWithHash = insertHashEnum(spec, hash)

    val targetPath = Paths.get(outputPath)
    Files.createDirectories(targetPath.parent)
    Files.writeString(targetPath, specWithHash)

    println("   -> ✅ Saved '$openApiSpecName' to '$outputPath' with embedded hash")
}

private fun sha256Of(spec: String): String = MessageDigest.getInstance("SHA-256")
    .digest(spec.toByteArray())
    .joinToString("") { "%02x".format(it) }

fun generateOpenApiHash(openApiSpecName: String): String {
    val spec = OpenApiPlugin.getOpenApiSpec(openApiSpecName)
    return sha256Of(spec)
}

private fun insertHashEnum(spec: String, hash: String): String {
    val lines = spec.lines().toMutableList()

    val schemasIndex = lines.indexOfFirst { it.trim() == "schemas:" }
    if (schemasIndex == -1) {
        throw IllegalStateException("Fant ikke \"schemas:\" block")
    }

    val indent = lines[schemasIndex].takeWhile { it == ' ' || it == '\t' }

    val schemaBlock = listOf(
        "$indent  OpenApiHash:",
        "$indent    type: string",
        "$indent    enum:",
        "$indent      - \"$hash\"",
    )

    lines.addAll(schemasIndex + 1, schemaBlock)

    return lines.joinToString("\n")
}
