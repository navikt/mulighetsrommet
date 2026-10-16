package no.nav.mulighetsrommet.admin.gjennomforing

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.avtale.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.domain.opplaring.Utdanningslop
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class GjennomforingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    val detaljer: GjennomforingDetaljerRequest,
    val veilederinformasjon: GjennomforingVeilederinfoRequest,
)

@Serializable
data class GjennomforingVeilederinfoRequest(
    val navRegioner: Set<NavEnhetNummer>,
    val navKontorer: Set<NavEnhetNummer>,
    val navAndreEnheter: Set<NavEnhetNummer>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val kontaktpersoner: Set<GjennomforingDetaljerRequest.Kontaktperson>,
)

@Serializable
data class SetStengtHosArrangorRequest(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate? = null,
    val beskrivelse: String? = null,
) {
    fun validate(): Either<NonEmptyList<FieldError>, Pair<Periode, String>> = either {
        zipOrAccumulate(
            {
                ensure(!beskrivelse.isNullOrBlank()) {
                    FieldError.of(
                        detail = "Du må legge inn en beskrivelse",
                        SetStengtHosArrangorRequest::beskrivelse,
                    )
                }
                beskrivelse
            },
            {
                ensureNotNull(periodeStart) {
                    FieldError.of(
                        detail = "Du må legge inn start på perioden",
                        SetStengtHosArrangorRequest::periodeStart,
                    )
                }
            },
            {
                ensureNotNull(periodeSlutt) {
                    FieldError.of(
                        detail = "Du må legge inn slutt på perioden",
                        SetStengtHosArrangorRequest::periodeSlutt,
                    )
                }
            },
        ) { beskrivelse, start, slutt ->
            ensure(!slutt.isBefore(start)) {
                FieldError.of(
                    detail = "Start må være før slutt",
                    SetStengtHosArrangorRequest::periodeStart,
                ).nel()
            }

            val periode = Periode.fromInclusiveDates(start, slutt)

            Pair(periode, beskrivelse)
        }
    }
}

@Serializable
data class SetTilgjengligForArrangorRequest(
    val tilgjengeligForArrangorDato: String?,
)

@Serializable
data class GjennomforingDetaljerRequest(
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val arrangorId: UUID?,
    val arrangorKontaktpersoner: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val administratorer: Set<NavIdent>,
    val oppstart: GjennomforingOppstartstype?,
    val oppmoteSted: String?,
    val deltidsprosent: Double,
    @Serializable(with = LocalDateSerializer::class)
    val tilgjengeligForArrangorDato: LocalDate?,
    val amoKategorisering: AmoKategoriseringRequest?,
    val utdanningslop: Utdanningslop? = null,
    @Serializable(with = UUIDSerializer::class)
    val prismodellId: UUID?,
    val pameldingType: GjennomforingPameldingType?,
) {
    @Serializable
    data class Kontaktperson(
        val navIdent: NavIdent,
        val beskrivelse: String?,
    )
}
