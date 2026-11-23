package no.nav.mulighetsrommet.api.gjennomforing.service

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.deltaker.DeltakelserError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelseFraKomet
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserRequest
import no.nav.mulighetsrommet.api.clients.amtDeltaker.DeltakelserResponse
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

class AmtDeltakelserPortTest : FunSpec({
    val ident = NorskIdent("12345678901")
    val accessType = AccessType.OBO.AzureAd("test-token")
    val onBehalfOf = PersonaliaService.OnBehalfOf.NavAnsatt(accessType)
    val aktivId = UUID.fromString("6b9847ad-9cba-4db4-b18f-c2ce70ba0151")
    val historiskId = UUID.fromString("b094cff0-9c83-4d38-a102-e7c3893e0349")
    fun deltakelse(id: UUID) = DeltakelseFraKomet(
        deltakerId = id,
        deltakerlisteId = UUID.fromString("b97b4f02-ae27-44d1-b180-2c2d8e085284"),
        tittel = "Opplæring",
        tiltakstype = DeltakelserResponse.Tiltakstype(
            "Enkeltplass AMO",
            Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
        ),
        status = DeltakelseFraKomet.Status(DeltakerStatusType.DELTAR, "Deltar"),
        oppstartstype = GjennomforingOppstartstype.ENKELTPLASS,
    )

    test("henter aktive og historiske deltakelser på vegne av saksbehandler uten duplikater") {
        val amt = mockk<AmtDeltakerClient>()
        val personalia = mockk<PersonaliaService>()
        coEvery { personalia.navAnsattTilgangTilPerson(ident, onBehalfOf) } returns null
        coEvery { amt.hentDeltakelser(DeltakelserRequest(ident), accessType) } returns DeltakelserResponse(
            aktive = listOf(deltakelse(aktivId)),
            historikk = listOf(deltakelse(historiskId), deltakelse(aktivId)),
        ).right()

        val port = AmtDeltakelserPort(amt, personalia)

        port.finnDeltakerIds(ident, accessType).shouldBeRight(listOf(aktivId, historiskId))
    }

    test("manglende deltakelser fra Komet gir tomt resultat") {
        val amt = mockk<AmtDeltakerClient>()
        val personalia = mockk<PersonaliaService>()
        coEvery { personalia.navAnsattTilgangTilPerson(ident, onBehalfOf) } returns null
        coEvery { amt.hentDeltakelser(DeltakelserRequest(ident), accessType) } returns AmtDeltakerError.NotFound.left()

        val port = AmtDeltakelserPort(amt, personalia)

        port.finnDeltakerIds(ident, accessType).shouldBeRight().shouldBeEmpty()
    }

    test("avviser uten å kontakte Komet når saksbehandler mangler tilgang til person") {
        val amt = mockk<AmtDeltakerClient>()
        val personalia = mockk<PersonaliaService>()
        coEvery { personalia.navAnsattTilgangTilPerson(ident, onBehalfOf) } returns AvvistGrunn.AVVIST_HABILITET

        val port = AmtDeltakelserPort(amt, personalia)

        port.finnDeltakerIds(ident, accessType).shouldBeLeft(
            DeltakelserError.ManglerTilgang(AvvistGrunn.AVVIST_HABILITET),
        )

        coVerify { amt wasNot Called }
    }

    test("feil fra Komet blir ikke behandlet som manglende treff") {
        val amt = mockk<AmtDeltakerClient>()
        val personalia = mockk<PersonaliaService>()
        coEvery { personalia.navAnsattTilgangTilPerson(ident, onBehalfOf) } returns null
        coEvery { amt.hentDeltakelser(DeltakelserRequest(ident), accessType) } returns AmtDeltakerError.Error.left()

        val port = AmtDeltakelserPort(amt, personalia)

        port.finnDeltakerIds(ident, accessType).shouldBeLeft(DeltakelserError.OppslagFeilet)
    }
})
