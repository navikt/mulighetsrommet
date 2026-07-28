package no.nav.mulighetsrommet.api.persistence

import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.NavAnsattFixture
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.PrismodellFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures

data class AdminTestDomain(
    val navEnheter: List<NavEnhet> = listOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
    val ansatte: List<NavAnsatt> = listOf(NavAnsattFixture.DonaldDuck, NavAnsattFixture.MikkeMus),
    val arrangorer: List<Arrangor> = listOf(
        ArrangorFixtures.hovedenhet,
        ArrangorFixtures.underenhet1,
        ArrangorFixtures.underenhet2,
    ),
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
    val prismodeller: List<Prismodell> = listOf(
        PrismodellFixtures.AnnenAvtaltPris,
        PrismodellFixtures.AvtaltPrisPerTimeOppfolging,
        PrismodellFixtures.ForhandsgodkjentAft,
        PrismodellFixtures.ForhandsgodkjentVtas,
    ),
    val avtaler: List<Avtale> = listOf(),
    val deltakere: List<Deltaker> = listOf(),
    val utdanningsprogram: List<Utdanningsprogram> = listOf(),
    val additionalSetup: (SqlQueryContext.(AdminTestDomain) -> Unit)? = null,
) {
    context(tx: SqlQueryContext)
    fun initialize(): AdminTestDomain {
        with(tx) {
            utdanningsprogram.forEach { utdanning.save(it) }
            regelverklenke.forEach { redaksjoneltInnholdLenke.upsert(it) }
            navEnheter.forEach { navEnhet.save(it) }
            ansatte.forEach { navAnsatt.save(it) }
            arrangorer.forEach { arrangor.save(it) }
            tiltakstyper.forEach { tiltakstype.save(it) }
            prismodeller.forEach { prismodell.upsert(it) }
            avtaler.forEach { avtale.save(it) }
            deltakere.forEach { deltaker.save(it) }
        }

        additionalSetup?.invoke(tx, this)

        return this
    }
}
