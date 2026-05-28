package no.nav.mulighetsrommet.api.amo

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellType
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.BransjeFixtures
import no.nav.mulighetsrommet.api.fixtures.ForerkortFixtures
import no.nav.mulighetsrommet.api.fixtures.KurstypeFixtures
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Gjovik
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures.Innlandet
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navenhet.Kontorstruktur
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class OpplaringKategoriseringValidatorTest : FunSpec({
    val avtale = Avtale(
        id = AvtaleFixtures.oppfolging.id,
        navn = "Avtalenavn",
        avtalenummer = "2023#1",
        sakarkivNummer = SakarkivNummer("24/1234"),
        tiltakstype = Avtale.Tiltakstype(
            navn = TiltakstypeFixtures.Oppfolging.navn,
            id = TiltakstypeFixtures.Oppfolging.id,
            tiltakskode = TiltakstypeFixtures.Oppfolging.tiltakskode,
        ),
        arrangor = Avtale.ArrangorHovedenhet(
            id = ArrangorFixtures.hovedenhet.id,
            organisasjonsnummer = ArrangorFixtures.hovedenhet.organisasjonsnummer,
            navn = ArrangorFixtures.hovedenhet.navn,
            underenheter = listOf(
                Avtale.ArrangorUnderenhet(
                    navn = ArrangorFixtures.underenhet1.navn,
                    id = ArrangorFixtures.underenhet1.id,
                    organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                    slettet = false,
                ),
            ),
            kontaktpersoner = emptyList(),
            slettet = false,
        ),
        startDato = LocalDate.now(),
        sluttDato = LocalDate.now().plusMonths(1),
        status = AvtaleStatus.Aktiv,
        avtaletype = Avtaletype.RAMMEAVTALE,
        administratorer = emptyList(),
        kontorstruktur = listOf(
            Kontorstruktur(
                region = Kontorstruktur.Region(Innlandet.navn, Innlandet.enhetsnummer),
                kontorer = listOf(
                    Kontorstruktur.Kontor(Gjovik.navn, Gjovik.enhetsnummer, Kontorstruktur.Kontortype.LOKAL),
                ),
            ),
        ),
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = emptyList(),
        personvernBekreftet = false,
        amoKategorisering = null,
        opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
        utdanningslop = null,
        prismodeller = listOf(
            Prismodell.AnnenAvtaltPris(
                id = UUID.randomUUID(),
                valuta = Valuta.NOK,
                prisbetingelser = null,
                tilsagnPerDeltaker = false,
            ),
        ),
        opsjonerRegistrert = emptyList(),
    )

    val ctx = OpplaringKategoriseringValiator.Context(
        kurstyper = KurstypeFixtures.all(),
        bransjer = BransjeFixtures.all(),
        forerkort = ForerkortFixtures.all(),
        utdanningsprogram = emptyList(), // TODO
    )

    context("avtale validering") {
    }

    context("gjennomføring validering") {
        test("amoKategorisering er påkrevd for avtale og gjennomføring når tiltakstype er Gruppe AMO") {
            val avtaleUtenAmokategorisering = avtale.copy(
                tiltakstype = Avtale.Tiltakstype(
                    tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
                    id = TiltakstypeFixtures.GruppeAmo.id,
                    navn = TiltakstypeFixtures.GruppeAmo.navn,
                ),
                amoKategorisering = null,
            )

            context(ctx) {
                OpplaringKategoriseringValiator.validateGjennomforingKategorisering(
                    avtaleUtenAmokategorisering,
                    request = null,
                ).shouldBeLeft(
                    listOf(
                        FieldError("/avtaleId", "Du må velge kurstype for avtalen"),
                        FieldError("/amoKategorisering/kurstypeId", "Du må velge en kurstype"),
                    ),
                )
            }
        }

        test("Kurselement må velges for gjennomføring når tiltakstype er Gruppe AMO") {
            val avtaleUtenAmokategorisering = avtale.copy(
                tiltakstype = Avtale.Tiltakstype(
                    tiltakskode = TiltakstypeFixtures.GruppeAmo.tiltakskode,
                    id = TiltakstypeFixtures.GruppeAmo.id,
                    navn = TiltakstypeFixtures.GruppeAmo.navn,
                ),
                amoKategorisering = AmoKategorisering(kurstype = KurstypeFixtures.studiespesialisering),
            )

            context(ctx) {
                OpplaringKategoriseringValiator.validateGjennomforingKategorisering(
                    avtaleUtenAmokategorisering,
                    request = null,
                ).shouldBeLeft(
                    listOf(
                        FieldError("/amoKategorisering/kurstypeId", "Du må velge en kurstype"),
                    ),
                )
            }
        }

        test("utdanningsprogram og lærefag er påkrevd når tiltakstypen er Gruppe Fag- og yrkesopplæring") {
            val avtaleGruFag = avtale.copy(
                tiltakstype = Avtale.Tiltakstype(
                    tiltakskode = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.tiltakskode,
                    id = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.id,
                    navn = TiltakstypeFixtures.GruppeFagOgYrkesopplaering.navn,
                ),
            )

            OpplaringKategoriseringValiator.validateGjennomforingUtdanningslop(
                avtaleGruFag,
                utdanningslop = null,
            ).shouldBeLeft(
                listOf(FieldError("/utdanningslop", "Du må velge utdanningsprogram og lærefag på avtalen")),
            )
        }
    }
})
