package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingAdminDto
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeAdminDto
import no.nav.mulighetsrommet.api.repositories.ArrangorRepository
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AvtaleStatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import java.time.LocalDate

class TiltaksgjennomforingValidator(
    private val tiltakstyper: TiltakstypeService,
    private val avtaler: AvtaleRepository,
    private val arrangorer: ArrangorRepository,
) {
    val maksAntallTegnStedForGjennomforing = 100
    fun validate(
        dbo: TiltaksgjennomforingDbo,
        previous: TiltaksgjennomforingAdminDto?,
    ): Either<List<ValidationError>, TiltaksgjennomforingDbo> = either {
        val tiltakstype = tiltakstyper.getById(dbo.tiltakstypeId)
            ?: raise(ValidationError.of(TiltaksgjennomforingDbo::tiltakstypeId, "Tiltakstypen finnes ikke").nel())

        if (isTiltakstypeDisabled(previous, tiltakstype)) {
            return ValidationError
                .of(
                    TiltaksgjennomforingDbo::avtaleId,
                    "Opprettelse av tiltaksgjennomføring for tiltakstype: '${tiltakstype.navn}' er ikke skrudd på enda.",
                )
                .nel()
                .left()
        }

        val avtale = avtaler.get(dbo.avtaleId)
            ?: raise(ValidationError.of(TiltaksgjennomforingDbo::avtaleId, "Avtalen finnes ikke").nel())

        val errors = buildList {
            if (avtale.tiltakstype.id != dbo.tiltakstypeId) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::tiltakstypeId,
                        "Tiltakstypen må være den samme som for avtalen",
                    ),
                )
            }

            if (dbo.administratorer.isEmpty()) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::administratorer,
                        "Minst én administrator må være valgt",
                    ),
                )
            }

            if (avtale.avtaletype != Avtaletype.Forhaandsgodkjent && dbo.sluttDato == null) {
                add(ValidationError.of(AvtaleDbo::sluttDato, "Sluttdato må være satt"))
            }

            if (dbo.sluttDato != null && dbo.startDato.isAfter(dbo.sluttDato)) {
                add(ValidationError.of(TiltaksgjennomforingDbo::startDato, "Startdato må være før sluttdato"))
            }

            if (dbo.antallPlasser <= 0) {
                add(ValidationError.of(TiltaksgjennomforingDbo::antallPlasser, "Antall plasser må være større enn 0"))
            }

            if (Tiltakskoder.isKursTiltak(avtale.tiltakstype.arenaKode)) {
                validateKursTiltak(dbo)
            } else {
                if (dbo.oppstart == TiltaksgjennomforingOppstartstype.FELLES) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::oppstart,
                            "Tiltaket må ha løpende oppstartstype",
                        ),
                    )
                }
            }

            if (dbo.navEnheter.isEmpty()) {
                add(ValidationError.of(TiltaksgjennomforingDbo::navEnheter, "Minst ett NAV-kontor må være valgt"))
            }

            if (!avtale.kontorstruktur.any { it.region.enhetsnummer == dbo.navRegion }) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::navEnheter,
                        "NAV-region ${dbo.navRegion} mangler i avtalen",
                    ),
                )
            }

            val avtaleNavEnheter = avtale.kontorstruktur.flatMap { it.kontorer }.associateBy { it.enhetsnummer }
            dbo.navEnheter.forEach { enhetsnummer ->
                if (!avtaleNavEnheter.containsKey(enhetsnummer)) {
                    add(
                        ValidationError.of(
                            TiltaksgjennomforingDbo::navEnheter,
                            "NAV-enhet $enhetsnummer mangler i avtalen",
                        ),
                    )
                }
            }

            val avtaleHasArrangor = avtale.arrangor.underenheter.any {
                it.id == dbo.arrangorId
            }
            if (!avtaleHasArrangor) {
                add(ValidationError.of(TiltaksgjennomforingDbo::arrangorId, "Arrangøren mangler i avtalen"))
            }

            if (avtale.tiltakstype.arenaKode == Tiltakskode.toArenaKode(Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING)) {
                val nusdata = dbo.nusData?.let { Json.decodeFromJsonElement<TiltaksgjennomforingAdminDto.NusData>(it) }
                if (nusdata != null && nusdata.utdanningskategorier.isEmpty()) {
                    add(ValidationError.of(TiltaksgjennomforingDbo::nusData, "Du må velge minst én utdanningskategori"))
                }
            }

            if (previous == null) {
                validateCreateGjennomforing(dbo, avtale)
            } else {
                validateUpdateGjennomforing(dbo, previous, avtale)
            }
        }

        return errors.takeIf { it.isNotEmpty() }?.left() ?: dbo.right()
    }

    private fun MutableList<ValidationError>.validateCreateGjennomforing(
        gjennomforing: TiltaksgjennomforingDbo,
        avtale: AvtaleAdminDto,
    ) {
        val arrangor = arrangorer.getById(gjennomforing.arrangorId)
        if (arrangor.slettetDato != null) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::arrangorId,
                    "Arrangøren ${arrangor.navn} er slettet i Brønnøysundregistrene. Gjennomføringer kan ikke opprettes for slettede bedrifter.",
                ),
            )
        }

        if (gjennomforing.startDato.isBefore(avtale.startDato)) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::startDato,
                    "Startdato må være etter avtalens startdato",
                ),
            )
        }

        if (gjennomforing.stedForGjennomforing != null && gjennomforing.stedForGjennomforing.length > maksAntallTegnStedForGjennomforing) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::stedForGjennomforing,
                    "Du kan bare skrive $maksAntallTegnStedForGjennomforing tegn i \"Sted for gjennomføring\"",
                ),
            )
        }

        if (avtale.status != AvtaleStatus.AKTIV) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::avtaleId,
                    "Avtalen må være aktiv for å kunne opprette tiltak",
                ),
            )
        }
    }

    private fun MutableList<ValidationError>.validateUpdateGjennomforing(
        gjennomforing: TiltaksgjennomforingDbo,
        previous: TiltaksgjennomforingAdminDto,
        avtale: AvtaleAdminDto,
    ) {
        if (!previous.isAktiv()) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::navn,
                    "Kan bare gjøre endringer når gjennomføringen er aktiv",
                ),
            )
        }

        if (gjennomforing.arrangorId != previous.arrangor.id) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::arrangorId,
                    "Arrangøren kan ikke endres når gjennomføringen er aktiv",
                ),
            )
        }

        if (previous.status is TiltaksgjennomforingStatus.GJENNOMFORES) {
            if (gjennomforing.avtaleId != previous.avtaleId) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::avtaleId,
                        "Avtalen kan ikke endres når gjennomføringen er aktiv",
                    ),
                )
            }

            if (gjennomforing.startDato.isBefore(avtale.startDato)) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::startDato,
                        "Startdato må være etter avtalens startdato",
                    ),
                )
            }

            if (gjennomforing.sluttDato != null && previous.sluttDato != null && gjennomforing.sluttDato.isBefore(
                    LocalDate.now(),
                )
            ) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::sluttDato,
                        "Sluttdato kan ikke endres bakover i tid når gjennomføringen er aktiv",
                    ),
                )
            }
        }

        if (isOwnedByArena(previous)) {
            if (gjennomforing.navn != previous.navn) {
                add(ValidationError.of(TiltaksgjennomforingDbo::navn, "Navn kan ikke endres utenfor Arena"))
            }

            if (gjennomforing.startDato != previous.startDato) {
                add(ValidationError.of(TiltaksgjennomforingDbo::startDato, "Startdato kan ikke endres utenfor Arena"))
            }

            if (gjennomforing.sluttDato != previous.sluttDato) {
                add(ValidationError.of(TiltaksgjennomforingDbo::sluttDato, "Sluttdato kan ikke endres utenfor Arena"))
            }

            if (gjennomforing.apentForInnsok != previous.apentForInnsok) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::apentForInnsok,
                        "Åpent for innsøk kan ikke endres utenfor Arena",
                    ),
                )
            }

            if (gjennomforing.antallPlasser != previous.antallPlasser) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::antallPlasser,
                        "Antall plasser kan ikke endres utenfor Arena",
                    ),
                )
            }

            if (gjennomforing.deltidsprosent != previous.deltidsprosent) {
                add(
                    ValidationError.of(
                        TiltaksgjennomforingDbo::deltidsprosent,
                        "Deltidsprosent kan ikke endres utenfor Arena",
                    ),
                )
            }
        }
    }

    private fun MutableList<ValidationError>.validateKursTiltak(dbo: TiltaksgjennomforingDbo) {
        if (dbo.deltidsprosent <= 0) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Deltidsprosent må være større enn 0",
                ),
            )
        } else if (dbo.deltidsprosent > 100) {
            add(
                ValidationError.of(
                    TiltaksgjennomforingDbo::deltidsprosent,
                    "Deltidsprosent kan ikke være større enn 100",
                ),
            )
        }
    }

    private fun isTiltakstypeDisabled(
        previous: TiltaksgjennomforingAdminDto?,
        tiltakstype: TiltakstypeAdminDto,
    ) = previous == null && !tiltakstyper.isEnabled(Tiltakskode.fromArenaKode(tiltakstype.arenaKode))

    private fun isOwnedByArena(previous: TiltaksgjennomforingAdminDto): Boolean {
        val tiltakskode = Tiltakskode.fromArenaKode(previous.tiltakstype.arenaKode)
        return previous.opphav == ArenaMigrering.Opphav.ARENA && !tiltakstyper.isEnabled(tiltakskode)
    }
}
