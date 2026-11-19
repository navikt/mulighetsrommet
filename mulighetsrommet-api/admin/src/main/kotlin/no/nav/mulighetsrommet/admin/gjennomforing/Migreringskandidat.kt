package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.left
import arrow.core.nel
import arrow.core.right
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
import java.time.LocalDate

internal data class Migreringskandidat(
    val gjennomforing: GjennomforingArena,
    val deltaker: Deltaker,
    val ansvarligEnhet: NavEnhetNummer,
)

internal val tiltakskoderForArenaMigrering = setOf(
    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING,
    Tiltakskode.HOYERE_UTDANNING,
)

internal fun QueryContext.validerMigreringskandidat(
    gjennomforing: GjennomforingArena,
    tiltakstyper: TiltakstypeService,
): Validated<Migreringskandidat> {
    val tiltakskode = gjennomforing.tiltakstype.tiltakskode
    if (tiltakskode !in tiltakskoderForArenaMigrering) {
        return FieldError.of("Tiltakstypen støtter ikke migrering fra Arena").nel().left()
    }
    if (!tiltakstyper.erMigrert(tiltakskode)) {
        return FieldError.of("Tiltakstypen er ikke migrert").nel().left()
    }
    val deltaker = repository.deltaker.getByGjennomforing(gjennomforing.id).singleOrNull()
        ?: return FieldError.of("Gjennomføringen må ha nøyaktig én deltaker").nel().left()
    if (!deltaker.erRelevantForMigrering()) {
        return FieldError.of("Deltakeren har ikke en relevant status og periode for migrering fra Arena").nel().left()
    }
    val ansvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet?.let(::NavEnhetNummer)
        ?: return FieldError.of("Gjennomføringen mangler ansvarlig Nav-enhet fra Arena").nel().left()
    return Migreringskandidat(gjennomforing, deltaker, ansvarligEnhet).right()
}

private fun Deltaker.erRelevantForMigrering(): Boolean {
    val cutoff = LocalDate.of(2026, 1, 1)
    val sluttDato = sluttDato
    return when (status.type) {
        DeltakerStatusType.UTKAST_TIL_PAMELDING,
        DeltakerStatusType.SOKT_INN,
        DeltakerStatusType.VENTER_PA_OPPSTART,
        DeltakerStatusType.DELTAR,
        -> sluttDato == null || sluttDato >= cutoff

        DeltakerStatusType.FULLFORT,
        DeltakerStatusType.AVBRUTT,
        -> sluttDato != null && sluttDato >= cutoff

        else -> false
    }
}
