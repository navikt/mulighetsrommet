package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import no.nav.poao_tilgang.client.api.ApiResult
import java.util.*

class PoaoTilgangServiceTest : FunSpec(
    {
        val navAnsattAzureId1 = UUID.randomUUID()
        val navAnsattAzureId2 = UUID.randomUUID()

        context("verifyAccessToBrukerForVeileder") {
            test("should throw StatusException when decision is DENY") {
                val mockResponse = ApiResult<Decision>(
                    throwable = null,
                    result = Decision.Deny(
                        message = "",
                        reason = "",
                    ),
                )

                val client: PoaoTilgangClient = mockk()
                every {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                } returns mockResponse

                val service = PoaoTilgangService(client)

                shouldThrow<StatusException> {
                    service.verifyAccessToUserFromVeileder(navAnsattAzureId1, NorskIdent("12345678910"))
                }

                verify(exactly = 1) {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                }
            }

            test("should verify access to modia when decision is PERMIT") {
                val mockResponse = ApiResult<Decision>(
                    throwable = null,
                    result = Decision.Permit,
                )

                val client: PoaoTilgangClient = mockk()
                every {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                } returns mockResponse

                val service = PoaoTilgangService(client)

                service.verifyAccessToUserFromVeileder(navAnsattAzureId1, NorskIdent("12345678910"))

                verify(exactly = 1) {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                }
            }

            test("should cache response based on provided NAVident") {
                val mockResponse = ApiResult<Decision>(
                    throwable = null,
                    result = Decision.Permit,
                )

                val client: PoaoTilgangClient = mockk()
                every {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                } returns mockResponse
                every {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId2,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                } returns mockResponse
                every {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId2,
                            TilgangType.LESE,
                            "10987654321",
                        ),
                    )
                } returns mockResponse

                val service = PoaoTilgangService(client)

                service.verifyAccessToUserFromVeileder(navAnsattAzureId1, NorskIdent("12345678910"))
                service.verifyAccessToUserFromVeileder(navAnsattAzureId1, NorskIdent("12345678910"))

                service.verifyAccessToUserFromVeileder(navAnsattAzureId2, NorskIdent("12345678910"))
                service.verifyAccessToUserFromVeileder(navAnsattAzureId2, NorskIdent("12345678910"))
                service.verifyAccessToUserFromVeileder(navAnsattAzureId2, NorskIdent("10987654321"))

                verify(exactly = 1) {
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId1,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId2,
                            TilgangType.LESE,
                            "12345678910",
                        ),
                    )
                    client.evaluatePolicy(
                        NavAnsattTilgangTilEksternBrukerPolicyInput(
                            navAnsattAzureId2,
                            TilgangType.LESE,
                            "10987654321",
                        ),
                    )
                }
            }
        }
    },
)
