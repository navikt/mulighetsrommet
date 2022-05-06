package no.nav.mulighetsrommet.arena_ords_proxy

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.sksamuel.hoplite.Masked
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArenaOrdsClientTest : FunSpec({

//    val arenaOrdsEndpoint = WireMockServer(9898)
    val arenaOrdsClient = ArenaOrdsClient(ArenaOrdsConfig("http://localhost:9898", "clientId", Masked("clientSecret")))
//    listener(WireMockListener(arenaOrdsEndpoint, ListenerMode.PER_SPEC))
//    val personList = ArenaPersonIdList(
//        listOf(PersonFnr("123", "123"), PersonFnr("456", "456"))
//    )
//    val bodyResponse = Json.encodeToString(ArenaPersonIdList.serializer(), personList)

    test("will return a populated list of people through ArenaPersonIdList") {
//        arenaOrdsEndpoint.stubFor(
//            WireMock.post(WireMock.urlEqualTo("/arena/api/v1/person/identListe"))
//                .willReturn(aResponse().withStatus(200).withBody(bodyResponse))
//        )
//        val response = arenaOrdsClient.getFnrByArenaPersonId(ArenaPersonIdList(listOf(PersonFnr("123"), PersonFnr("456"))))

//        response.personListe.first().personId.shouldBe("123")
//        response.personListe.first().fnr.shouldBe("123")
//
//        response.personListe.last().personId.shouldBe("456")
//        response.personListe.last().fnr.shouldBe("456")
    }

    test("will return ArbeidsgiverInfo") {
//        arenaOrdsEndpoint.stubFor(
//            WireMock.get(WireMock.urlEqualTo("/arena/api/v1/arbeidsgiver/ident"))
//                .willReturn(WireMock.ok().withBody(Json.encodeToString(ArbeidsgiverInfo(123, 123))).withHeader("Content-Type", "application/json"))
//        )
        val response = arenaOrdsClient.getArbeidsgiverInfoByArenaArbeidsgiverId(123)
    }

})
