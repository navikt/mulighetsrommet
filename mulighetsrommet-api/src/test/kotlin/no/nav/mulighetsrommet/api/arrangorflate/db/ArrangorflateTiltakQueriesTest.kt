package no.nav.mulighetsrommet.api.arrangorflate.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterType
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.time.LocalDate
import java.util.UUID

class ArrangorflateTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val aft2 =
        AFT1.copy(
            id = UUID.randomUUID(),
            startDato = LocalDate.of(2021, 1, 1),
            sluttDato = LocalDate.of(2022, 12, 31),
        )
    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
        gjennomforinger = listOf(
            Oppfolging1,
            AFT1,
            aft2,
        ),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    test("henter ingen tiltak når påkrevde argumenter mangler") {
        database.runAndRollback {
            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
                filter = ArrangorflateTiltakFilter(type = ArrangorflateFilterType.AKTIVE),
            ).items.shouldBeEmpty()

            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = setOf(),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
                filter = ArrangorflateTiltakFilter(type = ArrangorflateFilterType.AKTIVE),
            ).items.shouldBeEmpty()

            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(),
                filter = ArrangorflateTiltakFilter(type = ArrangorflateFilterType.AKTIVE),
            ).items.shouldBeEmpty()
        }
    }

    test("henter alle tiltak") {
        database.runAndRollback {
            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
                filter = ArrangorflateTiltakFilter(),
            ).items shouldContainExactlyIds listOf(AFT1.id, aft2.id)

            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.Oppfolging.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(
                    PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                ),
                filter = ArrangorflateTiltakFilter(),
            ).items shouldContainExactlyIds listOf(AFT1.id, aft2.id, Oppfolging1.id)

            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.Oppfolging.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(
                    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                ),
                filter = ArrangorflateTiltakFilter(),
            ).items shouldContainExactlyIds listOf(Oppfolging1.id)
        }
    }

    test("henter fra cutoff dato") {
        database.runAndRollback {
            var cutoff = LocalDate.of(2022, 1, 1)
            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
                filter = ArrangorflateTiltakFilter(sluttDatoGreaterThanOrEqualTo = cutoff),
            ).items shouldContainExactlyIds listOf(AFT1.id, aft2.id)

            cutoff = LocalDate.of(2023, 1, 1)
            queries.arrangorflate.tiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
                filter = ArrangorflateTiltakFilter(sluttDatoGreaterThanOrEqualTo = cutoff),
            ).items shouldContainExactlyIds listOf(AFT1.id)
        }
    }
})

private infix fun Collection<ArrangorflateTiltak>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
