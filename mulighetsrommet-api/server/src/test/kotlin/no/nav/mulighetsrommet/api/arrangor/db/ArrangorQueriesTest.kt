package no.nav.mulighetsrommet.api.arrangor.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorKobling
import no.nav.mulighetsrommet.admin.arrangor.toDto
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import java.util.UUID

// TODO: flyttes til "admin" etter at avhengigheter også er flyttet (avtale, gjennomføring)
class ArrangorQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    context("crud") {
        test("Filter på avtale eller gjennomforing") {
            val hovedenhet = ArrangorFixtures.hovedenhet
            val underenhet = ArrangorFixtures.underenhet1

            val domain = MulighetsrommetTestDomain(
                arrangorer = listOf(hovedenhet, underenhet),
                tiltakstyper = listOf(TiltakstypeFixtures.Oppfolging),
                avtaler = listOf(AvtaleFixtures.oppfolging),
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            )

            database.runAndRollback {
                domain.initialize()

                queries.arrangor.getAll().items shouldContainExactlyInAnyOrder listOf(
                    hovedenhet.toDto(),
                    underenhet.toDto(),
                )
                queries.arrangor.getAll(kobling = ArrangorKobling.AVTALE).should {
                    it.items shouldContainExactlyIds listOf(hovedenhet.id)
                }
                queries.arrangor.getAll(kobling = ArrangorKobling.TILTAKSGJENNOMFORING).should {
                    it.items shouldContainExactlyIds listOf(underenhet.id)
                }
            }
        }
    }
})

private infix fun Collection<ArrangorDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
