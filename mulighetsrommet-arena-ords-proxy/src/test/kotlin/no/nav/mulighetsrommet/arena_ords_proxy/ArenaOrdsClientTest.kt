package no.nav.mulighetsrommet.arena_ords_proxy

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArenaOrdsClientTest : FunSpec({

    val arenaOrdsEndpoint = WireMockServer(9898)
    val arenaOrdsClient = ArenaOrdsClient(ArenaOrdsConfig("http://localhost:9898", "clientId", Masked("clientSecret")))
    listener(WireMockListener(arenaOrdsEndpoint, ListenerMode.PER_SPEC))

    test("should return a populated list of people ArenaPersonIdList") {
        val personList = ArenaPersonIdList(
            listOf(PersonFnr("123", "123"), PersonFnr("456", "456"))
        )
        arenaOrdsEndpoint.stubFor(
            WireMock.post(WireMock.urlEqualTo("/arena/api/v1/person/identListe"))
                .willReturn(WireMock.ok().withBody(Json.encodeToString(personList)).withHeader("Content-Type", "application/json"))
        )
        val response = arenaOrdsClient.getFnrByArenaPersonId(ArenaPersonIdList(listOf(PersonFnr("123"), PersonFnr("456"))))

        response.personListe.first().personId.shouldBe("123")
        response.personListe.first().fnr.shouldBe("123")
        response.personListe.last().personId.shouldBe("456")
        response.personListe.last().fnr.shouldBe("456")
    }

    test("should return ArbeidsgiverInfo") {
        arenaOrdsEndpoint.stubFor(
            WireMock.get(WireMock.urlEqualTo("/arena/api/v1/arbeidsgiver/ident"))
                .willReturn(WireMock.ok().withBody(Json.encodeToString(ArbeidsgiverInfo(123, 123))).withHeader("Content-Type", "application/json"))
        )
        val response = arenaOrdsClient.getArbeidsgiverInfoByArenaArbeidsgiverId(123)
        response.bedriftsnr.shouldBe(123)
    }

})
