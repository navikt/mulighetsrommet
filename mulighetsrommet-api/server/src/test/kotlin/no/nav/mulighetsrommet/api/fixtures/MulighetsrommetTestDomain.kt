package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingLinjeDbo

data class MulighetsrommetTestDomain(
    val navEnheter: List<NavEnhet> = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
    val ansatte: List<NavAnsatt> = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
    val arrangorer: List<ArrangorDto> = listOf(
        ArrangorFixtures.hovedenhet,
        ArrangorFixtures.underenhet1,
        ArrangorFixtures.underenhet2,
    ),
    val arrangorKontaktpersoner: List<ArrangorKontaktperson> = listOf(),
    val tiltakstyper: List<Tiltakstype> = listOf(
        TiltakstypeFixtures.Oppfolging,
        TiltakstypeFixtures.Arbeidstrening,
        TiltakstypeFixtures.VTA,
        TiltakstypeFixtures.Jobbklubb,
        TiltakstypeFixtures.AFT,
        TiltakstypeFixtures.EnkelAmo,
        TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
        TiltakstypeFixtures.GruppeAmo,
        TiltakstypeFixtures.DigitalOppfolging,
        TiltakstypeFixtures.ArbeidsrettetRehabilitering,
        TiltakstypeFixtures.NorskGrunnFOV,
        TiltakstypeFixtures.EnkelFagOgYrke,
    ),
    val regelverklenke: List<RedaksjoneltInnholdLenke> = listOf(),
    val prismodeller: List<PrismodellDbo> = listOf(
        PrismodellFixtures.AnnenAvtaltPris,
        PrismodellFixtures.AvtaltPrisPerTimeOppfolging,
        PrismodellFixtures.ForhandsgodkjentAft,
        PrismodellFixtures.ForhandsgodkjentVtas,
    ),
    val avtaler: List<AvtaleDbo> = listOf(),
    val gjennomforinger: List<GjennomforingDbo> = listOf(),
    val deltakere: List<DeltakerDbo> = listOf(),
    val tilsagn: List<TilsagnDbo> = listOf(),
    val utbetalinger: List<UtbetalingDbo> = listOf(),
    val utbetalingLinjer: List<UtbetalingLinjeDbo> = listOf(),
    val additionalSetup: (QueryContext.(MulighetsrommetTestDomain) -> Unit)? = null,
) {
    fun initialize(database: ApiDatabase): MulighetsrommetTestDomain = database.transaction {
        initialize()
    }

    context(tx: QueryContext)
    fun initialize(): MulighetsrommetTestDomain {
        with(tx) {
            session.execute(KurstypeFixtures.query())
            session.execute(BransjeFixtures.query())
            session.execute(InnholdElementFixtures.query())
            session.execute(ForerkortFixtures.query())
            session.execute(UtdanningFixtures.UtdanningsProgram.query())
            session.execute(UtdanningFixtures.Utdanninger.query())

            navEnheter.forEach { queries.enhet.save(it) }
            ansatte.forEach { queries.ansatt.save(it) }
            arrangorer.forEach { dto ->
                val kontaktpersoner = arrangorKontaktpersoner.filter { it.arrangorId == dto.id }
                queries.arrangor.save(dto.toArrangor().copy(kontaktpersoner = kontaktpersoner))
            }
            tiltakstyper.forEach { repository.tiltakstype.save(it) }
            prismodeller.forEach { queries.prismodell.upsert(it) }
            regelverklenke.forEach { queries.regelverklenke.upsert(it) }
            avtaler.forEach { queries.avtale.create(it) }
            gjennomforinger.forEach { gjennomforing ->
                queries.gjennomforing.upsert(gjennomforing)
                // Sett gjennomforing FTS, siden den er brukt i andre søk (navn/tiltaksnavn)
                val tiltakstypeNavn = tiltakstyper.first { gjennomforing.tiltakstypeId == it.id }.navn
                queries.gjennomforing.setFreeTextSearch(gjennomforing.id, listOf(gjennomforing.navn, tiltakstypeNavn))
            }
            deltakere.forEach { queries.deltaker.upsert(it) }
            tilsagn.forEach { queries.tilsagn.upsert(it) }
            utbetalinger.forEach { queries.utbetaling.upsert(it) }
            utbetalingLinjer.forEach { queries.utbetalingLinje.upsert(it) }
        }

        additionalSetup?.invoke(tx, this)

        return this
    }
}
