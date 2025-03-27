package no.nav.mulighetsrommet.api.veilederflate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.AFT1
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.ArbeidsrettetRehabilitering
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Oslo
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Innsatsgruppe
import java.util.*

class VeilederflateTiltakQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("getAll") {
        val oppfolgingSanityId = UUID.randomUUID()
        val arbeidstreningSanityId = UUID.randomUUID()

        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(Innlandet, Gjovik, Oslo),
            tiltakstyper = listOf(TiltakstypeFixtures.AFT, TiltakstypeFixtures.Oppfolging),
            avtaler = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.AFT),
            gjennomforinger = listOf(Oppfolging1, AFT1),
        ) {
            session.execute(Query("update tiltakstype set sanity_id = '$oppfolgingSanityId' where id = '${TiltakstypeFixtures.Oppfolging.id}'"))
            session.execute(Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.AFT.id}'"))
            session.execute(Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE}'::innsatsgruppe]"))

            queries.gjennomforing.setPublisert(Oppfolging1.id, true)
            queries.gjennomforing.setPublisert(AFT1.id, true)
        }

        test("skal filtrere basert på om tiltaket er publisert") {
            database.runAndRollback { session ->
                domain.setup(session)

                val veilederflateTiltakQueries = VeilederflateTiltakQueries(session)

                veilederflateTiltakQueries.getAll(
                    brukersEnheter = listOf("0502"),
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ) shouldHaveSize 2

                queries.gjennomforing.setPublisert(Oppfolging1.id, false)

                veilederflateTiltakQueries.getAll(
                    brukersEnheter = listOf("0502"),
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ) shouldHaveSize 1

                queries.gjennomforing.setPublisert(AFT1.id, false)

                veilederflateTiltakQueries.getAll(
                    brukersEnheter = listOf("0502"),
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ) shouldHaveSize 0
            }
        }

        test("skal filtrere basert på innsatsgruppe") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = VeilederflateTiltakQueries(session)

                session.execute(Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE}'::innsatsgruppe] where id = '${TiltakstypeFixtures.Oppfolging.id}'"))
                session.execute(Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.GODE_MULIGHETER}'::innsatsgruppe, '${Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE}'::innsatsgruppe] where id = '${TiltakstypeFixtures.AFT.id}'"))

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.GODE_MULIGHETER,
                    brukersEnheter = listOf("0502"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
                    brukersEnheter = listOf("0502"),
                ) shouldHaveSize 2
            }
        }

        test("skal filtrere på brukers enheter") {
            database.runAndRollback { session ->
                domain.setup(session)
                queries.gjennomforing.upsert(Oppfolging1.copy(navEnheter = setOf("0400", "0502")))
                queries.gjennomforing.upsert(AFT1.copy(navEnheter = setOf("0400", "0300")))

                val queries = VeilederflateTiltakQueries(session)

                queries.getAll(
                    brukersEnheter = listOf("0502"),
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0300"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502", "0300"),
                ) shouldHaveSize 2

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0400"),
                ) shouldHaveSize 2
            }
        }

        test("skal filtrere basert på tiltakstype sanity Id") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = VeilederflateTiltakQueries(session)

                queries.getAll(
                    sanityTiltakstypeIds = null,
                    brukersEnheter = listOf("0502"),
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ) shouldHaveSize 2

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    sanityTiltakstypeIds = listOf(oppfolgingSanityId),
                    brukersEnheter = listOf("0502"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    sanityTiltakstypeIds = listOf(arbeidstreningSanityId),
                    brukersEnheter = listOf("0502"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }
            }
        }

        test("skal filtrere basert fritekst i navn") {
            database.runAndRollback { session ->
                domain.setup(session)
                queries.gjennomforing.upsert(Oppfolging1.copy(sluttDato = null, navn = "Oppfølging hos Erik"))
                queries.gjennomforing.upsert(AFT1.copy(navn = "AFT hos Frank"))

                val queries = VeilederflateTiltakQueries(session)

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502"),
                    search = "erik",
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502"),
                    search = "frank aft",
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502"),
                    search = "aft erik",
                ).should {
                    it shouldHaveSize 0
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502"),
                    search = "af",
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }
            }
        }

        test("skal filtrere basert på apent_for_pamelding") {
            database.runAndRollback { session ->
                domain.setup(session)
                queries.gjennomforing.setApentForPamelding(AFT1.id, false)

                val queries = VeilederflateTiltakQueries(session)

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    apentForPamelding = true,
                    brukersEnheter = listOf("0502"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(Oppfolging1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    apentForPamelding = false,
                    brukersEnheter = listOf("0502"),
                ).should {
                    it.shouldHaveSize(1).first().id.shouldBe(AFT1.id)
                }

                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    apentForPamelding = null,
                    brukersEnheter = listOf("0502"),
                ) shouldHaveSize 2
            }
        }
    }

    context("sykemeldt med arbeidsgiver") {
        val domain = MulighetsrommetTestDomain(
            navEnheter = listOf(Innlandet, Gjovik),
            tiltakstyper = listOf(TiltakstypeFixtures.ArbeidsrettetRehabilitering),
            avtaler = listOf(AvtaleFixtures.ArbeidsrettetRehabilitering),
            gjennomforinger = listOf(ArbeidsrettetRehabilitering),
        ) {
            session.execute(Query("update tiltakstype set sanity_id = '${UUID.randomUUID()}' where id = '${TiltakstypeFixtures.ArbeidsrettetRehabilitering.id}'"))
            session.execute(Query("update tiltakstype set innsatsgrupper = array ['${Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE}'::innsatsgruppe]"))

            queries.gjennomforing.setPublisert(ArbeidsrettetRehabilitering.id, true)
        }

        test("skal ta med ARR hvis sykmeldt med TRENGER_VEILEDNING") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = VeilederflateTiltakQueries(session)

                // Riktig innsatsgruppe
                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                    brukersEnheter = listOf("0502"),
                ).size shouldBe 1

                // Feil innsatsgruppe
                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING,
                    brukersEnheter = listOf("0502"),
                ).size shouldBe 0

                // Feil innsatsgruppe men sykmeldt
                queries.getAll(
                    innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING,
                    brukersEnheter = listOf("0502"),
                    erSykmeldtMedArbeidsgiver = true,
                ).size shouldBe 1
            }
        }
    }
})
