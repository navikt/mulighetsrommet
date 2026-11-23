package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.admin.tiltak.TiltakstypeService
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingArena
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.Validated
import java.time.LocalDate
import java.util.UUID

data class MigrerEnkeltplass(
    val id: UUID,
    val prismodell: UpsertEnkeltplass.Prismodell,
)

sealed interface MigrerEnkeltplassError {
    data class Valideringsfeil(val errors: List<FieldError>) : MigrerEnkeltplassError
    data class ManglerTilgang(val avvistGrunn: AvvistGrunn) : MigrerEnkeltplassError
}

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
): Validated<Migreringskandidat> = either {
    val tiltakskode = gjennomforing.tiltakstype.tiltakskode
    ensure(tiltakskode in tiltakskoderForArenaMigrering) {
        FieldError.of("Tiltakstypen støtter ikke migrering fra Arena").nel()
    }
    ensure(tiltakstyper.erMigrert(tiltakskode)) {
        FieldError.of("Tiltakstypen er ikke migrert").nel()
    }
    val deltaker = repository.deltaker.getByGjennomforing(gjennomforing.id).singleOrNull()
    ensureNotNull(deltaker) {
        FieldError.of("Deltakeren har ikke en relevant status og periode for migrering fra Arena").nel()
    }
    ensure(deltaker.erRelevantForMigrering()) {
        FieldError.of("Deltakeren har ikke en relevant status og periode for migrering fra Arena").nel()
    }
    val ansvarligEnhet = ensureNotNull(gjennomforing.arena?.ansvarligNavEnhet?.let(::NavEnhetNummer)) {
        FieldError.of("Gjennomføringen mangler ansvarlig Nav-enhet fra Arena").nel()
    }
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
