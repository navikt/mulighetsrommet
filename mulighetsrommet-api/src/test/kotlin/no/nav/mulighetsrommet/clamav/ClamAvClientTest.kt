package no.nav.mulighetsrommet.clamav

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson

class ClamAvClientTest : FunSpec({
    context("Virusscan av vedlegg") {
        test("Skal returnere OK når ingen virus er funnet") {
            val clamAvClient = ClamAvClient(
                baseUrl = "http://localhost:8080",
                clientEngine = createMockEngine {
                    post("http://localhost:8080/scan") {
                        respondJson(
                            listOf(
                                ScanResult(
                                    Filename = "test.txt",
                                    Result = Status.OK,
                                ),
                            ),
                        )
                    }
                },
            )

            val vedlegg = Vedlegg(
                content = Content(
                    contentType = "text/plain",
                    content = "test".toByteArray().decodeToString(),
                ),
                description = "test.txt",
            )
            val result = clamAvClient.virusScanVedlegg(listOf(vedlegg))
            result[0].Result shouldBe Status.OK
        }

        test("Skal returnere FOUND når virus er funnet") {
            val clamAvClient = ClamAvClient(
                baseUrl = "http://localhost:8080",
                clientEngine = createMockEngine {
                    post("http://localhost:8080/scan") {
                        respondJson(
                            listOf(
                                ScanResult(
                                    Filename = "test.txt",
                                    Result = Status.FOUND,
                                ),
                            ),
                        )
                    }
                },
            )

            val vedlegg = Vedlegg(
                content = Content(
                    contentType = "text/plain",
                    content = "test".toByteArray().decodeToString(),
                ),
                description = "test.txt",
            )
            val result = clamAvClient.virusScanVedlegg(listOf(vedlegg))
            result[0].Result shouldBe Status.FOUND
        }

        test("Skal returnere ERROR når virus er funnet") {
            val clamAvClient = ClamAvClient(
                baseUrl = "http://localhost:8080",
                clientEngine = createMockEngine {
                    post("http://localhost:8080/scan") {
                        respondJson(
                            listOf(
                                ScanResult(
                                    Filename = "test.txt",
                                    Result = Status.ERROR,
                                ),
                            ),
                        )
                    }
                },
            )

            val vedlegg = Vedlegg(
                content = Content(
                    contentType = "text/plain",
                    content = "test".toByteArray().decodeToString(),
                ),
                description = "test.txt",
            )
            val result = clamAvClient.virusScanVedlegg(listOf(vedlegg))
            result[0].Result shouldBe Status.ERROR
        }
    }
})
