package no.nav.mulighetsrommet.api.arrangorflate.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import java.util.UUID

class ArrangorflateTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        arrangorer = listOf(
            ArrangorFixtures.hovedenhet,
            ArrangorFixtures.underenhet1,
        ),
        avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.VTA, AvtaleFixtures.AFT),
        gjennomforinger = listOf(Oppfolging1, AFT1),
    )

    beforeSpec {
        domain.initialize(database.db)
    }

    test("henter ingen tiltak når påkrevde argumenter mangler") {
        database.runAndRollback {
            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
            ).shouldBeEmpty()

            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = listOf(),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
            ).shouldBeEmpty()

            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
            ).shouldBeEmpty()

            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(),
            ).shouldBeEmpty()
        }
    }

    test("henter alle tiltak") {
        database.runAndRollback {
            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK),
            ) shouldContainExactlyIds listOf(AFT1.id)

            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.Oppfolging.id),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(
                    PrismodellType.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK,
                    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                ),
            ) shouldContainExactlyIds listOf(AFT1.id, Oppfolging1.id)

            queries.arrangorTiltak.getAll(
                tiltakstyper = listOf(TiltakstypeFixtures.AFT.id, TiltakstypeFixtures.Oppfolging.id),
                organisasjonsnummer = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                statuser = listOf(GjennomforingStatusType.GJENNOMFORES),
                prismodeller = listOf(
                    PrismodellType.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER,
                ),
            ) shouldContainExactlyIds listOf(Oppfolging1.id)
        }
    }
})

private infix fun Collection<ArrangorflateTiltak>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
