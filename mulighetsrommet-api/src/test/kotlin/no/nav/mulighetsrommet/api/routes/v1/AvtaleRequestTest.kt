package no.nav.mulighetsrommet.api.routes.v1

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import java.time.LocalDate
import java.util.*

class AvtaleRequestTest : FunSpec({
    val avtale = AvtaleRequest(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakstypeId = UUID.randomUUID(),
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        leverandorKontaktpersonId = null,
        avtalenummer = "123456",
        startDato = LocalDate.of(2023, 6, 1),
        sluttDato = LocalDate.of(2024, 6, 1),
        navRegion = "2990",
        url = "http://localhost:8080",
        administrator = "B123456",
        avtaletype = Avtaletype.Avtale,
        prisOgBetalingsinformasjon = null,
        navEnheter = listOf("2990"),
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
    )

    test("request validation") {
        forAll(
            row(
                avtale.copy(sluttDato = LocalDate.of(2020, 1, 1)),
                BadRequest("Startdato må være før sluttdato"),
            ),
            row(
                avtale.copy(navEnheter = emptyList()),
                BadRequest("Minst ett NAV-kontor må være valgt"),
            ),
            row(
                avtale.copy(leverandorUnderenheter = emptyList()),
                BadRequest("Minst én underenhet til leverandøren må være valgt"),
            ),
        ) { request, result ->
            request.toDbo() shouldBeLeft result
        }
    }
})
