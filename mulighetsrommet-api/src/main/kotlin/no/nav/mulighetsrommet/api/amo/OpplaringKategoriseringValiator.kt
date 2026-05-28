package no.nav.mulighetsrommet.api.amo

import arrow.core.Either
import arrow.core.right
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringValiator.toDbo
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.mapper.toDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingDetaljerRequest
import no.nav.mulighetsrommet.api.gjennomforing.api.GjennomforingRequest
import no.nav.mulighetsrommet.api.responses.FieldError
import no.nav.mulighetsrommet.api.validation.FieldValidator
import no.nav.mulighetsrommet.api.validation.Validated
import no.nav.mulighetsrommet.api.validation.validation
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import no.nav.mulighetsrommet.utdanning.model.UtdanningsprogramMedUtdanninger
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object OpplaringKategoriseringValiator {
    data class Context(
        val kurstyper: Set<Kurstype>,
        val bransjer: Set<Bransje>,
        val forerkort: Set<ForerkortKlasse>,
        val utdanningsprogram: List<UtdanningsprogramMedUtdanninger>,
    )

    context(ctx: Context)
    fun FieldValidator.validateOpplaringKategorisering(
        tiltakskode: Tiltakskode,
        request: OpplaringKategoriseringRequest?,
    ): Either<List<FieldError>, AmoKategorisering?> = when (tiltakskode) {
        Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
            validateGruppeAmo(request)
        }

        Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
            validateAmo(request)
        }

        Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> {
            validateNorskopplaringGrunneleggendeFerdigheterFov(request)
        }

        Tiltakskode.FAG_OG_YRKESOPPLAERING,
        Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
        -> {
            validateUtdanningslop(request?.utdanningsprogramId, request?.larefag.orEmpty())
        }

        Tiltakskode.STUDIESPESIALISERING,
        -> {
            val studiespes = ctx.kurstyper.find { it.kode == Kurstype.Kode.STUDIESPESIALISERING }
            OpplaringKategoriseringRequest(kurstypeId = studiespes?.id).toDbo().right()
        }

        else -> Either.Right(null)
    }

    context(ctx: Context)
    private fun FieldValidator.validateGruppeAmo(request: OpplaringKategoriseringRequest?): Either<List<FieldError>, AmoKategorisering?> {
        requireValid(request?.kurstypeId != null && ctx.kurstyper.any { it.id == request.kurstypeId }) {
            FieldError.of(
                "Du må velge en kurstype",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::kurstypeId,
            )
        }
        val kurstype = ctx.kurstyper.find { it.id === request.kurstypeId }
        if (kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET) {
            requireValid(request.bransjeId != null && ctx.bransjer.any { it.id == request.bransjeId }) {
                FieldError.of(
                    "Du må velge en bransje",
                    DetaljerRequest::amoKategorisering,
                    OpplaringKategoriseringRequest::bransjeId,
                )
            }
        }
        return request.toDbo().right()
    }

    context(ctx: Context)
    private fun FieldValidator.validateAmo(request: OpplaringKategoriseringRequest?): Either<List<FieldError>, AmoKategorisering?> {
        requireValid(request?.bransjeId != null && ctx.bransjer.any { it.id == request.bransjeId }) {
            FieldError.of(
                "Du må velge en bransje",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::bransjeId,
            )
        }
        val kurstype = ctx.kurstyper.find { it.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET }
        return request.copy(kurstypeId = kurstype?.id).toDbo().right()
    }

    context(ctx: Context)
    private fun FieldValidator.validateNorskopplaringGrunneleggendeFerdigheterFov(request: OpplaringKategoriseringRequest?): Either<List<FieldError>, AmoKategorisering?> {
        requireValid(request?.kurstypeId != null) {
            FieldError.of(
                "Du må velge en kurstype",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::kurstypeId,
            )
        }
        val norskGrunnFov = setOf(
            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
            Kurstype.Kode.NORSKOPPLAERING,
            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER,
        )
        val kurstyper = ctx.kurstyper.filter { kurstype -> kurstype.kode in norskGrunnFov }.map { it.id }.toSet()
        validate(
            request.kurstypeId in kurstyper,
        ) {
            FieldError.of(
                "Ugyldig kurstype",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::kurstypeId,
            )
        }
        return request.toDbo().right()
    }

    context(ctx: Context)
    private fun FieldValidator.validateUtdanningslop(
        utdanningsProgramId: UUID?,
        larefag: List<UUID>,
    ): Either<List<FieldError>, AmoKategorisering?> {
        validateNotNull(utdanningsProgramId) {
            FieldError.of(
                "Du må velge et utdanningsprogram og minst ett lærefag",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::larefag,
            )
        }
        val valgtProgram = ctx.utdanningsprogram.find { it.utdanningsprogram.id == utdanningsProgramId }
        validateNotNull(valgtProgram) {
            FieldError.of(
                "Utdanningsprogramet er ugyldig, velg et fra listen",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::utdanningsprogramId,

            )
        }
        validate(larefag.isNotEmpty() || (valgtProgram != null && valgtProgram.utdanninger.any { it.id == larefag })) {
            FieldError.of(
                "Du må velge minst ett lærefag",
                DetaljerRequest::amoKategorisering,
                OpplaringKategoriseringRequest::larefag,
            )
        }

        return AmoKategorisering(utdanningslop = null).right()
    }

    context(ctx: Context)
    fun validateGjennomforingKategorisering(
        avtale: Avtale,
        request: OpplaringKategoriseringRequest?,
    ): Either<List<FieldError>, AmoKategorisering?> = validation {
        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.ARBEIDSMARKEDSOPPLAERING,
            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            Tiltakskode.STUDIESPESIALISERING,
            -> validate(avtale.amoKategorisering != null) {
                FieldError.of("Du må velge kurstype for avtalen", GjennomforingRequest::avtaleId)
            }

            else -> Unit
        }

        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING -> {
                requireValid(request?.kurstypeId != null) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingDetaljerRequest::amoKategorisering,
                        OpplaringKategoriseringRequest::kurstypeId,
                    )
                }
                val valgtKurstype = ctx.kurstyper.find { it.id == request.kurstypeId }
                if (valgtKurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET) {
                    requireValid(request.bransjeId != null) {
                        FieldError.of(
                            "Du må velge en bransje",
                            GjennomforingDetaljerRequest::amoKategorisering,
                            OpplaringKategoriseringRequest::bransjeId,
                        )
                    }
                }
                request
            }

            Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> {
                requireValid(request?.bransjeId != null && ctx.bransjer.any { it.id == request.bransjeId }) {
                    FieldError.of(
                        "Du må velge en bransje",
                        GjennomforingDetaljerRequest::amoKategorisering,
                        OpplaringKategoriseringRequest::bransjeId,
                    )
                }
                val kurstype = ctx.kurstyper.find { it.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET }
                request.copy(kurstypeId = kurstype?.id)
            }

            Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> {
                requireValid(request?.kurstypeId != null && ctx.kurstyper.any { it.id == request.kurstypeId }) {
                    FieldError.of(
                        "Du må velge en kurstype",
                        GjennomforingDetaljerRequest::amoKategorisering,
                        OpplaringKategoriseringRequest::kurstypeId,
                    )
                }
                request
            }

            Tiltakskode.STUDIESPESIALISERING,
            -> {
                val kurstype = ctx.kurstyper.find { it.kode == Kurstype.Kode.STUDIESPESIALISERING }
                OpplaringKategoriseringRequest(kurstypeId = kurstype?.id)
            }

            else -> null
        }?.toDbo()
    }

    fun validateGjennomforingUtdanningslop(
        avtale: Avtale,
        utdanningslop: UtdanningslopDbo?,
    ): Validated<UtdanningslopDbo?> = validation {
        when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
            Tiltakskode.FAG_OG_YRKESOPPLAERING,
            -> Unit

            else -> return@validation null
        }

        requireValid(utdanningslop != null) {
            FieldError.of(
                "Du må velge utdanningsprogram og lærefag på avtalen",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanninger.isNotEmpty()) {
            FieldError.of(
                "Du må velge minst ett lærefag",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        validate(utdanningslop.utdanningsprogram == avtale.utdanningslop?.utdanningsprogram?.id) {
            FieldError.of(
                "Utdanningsprogrammet må være det samme som for avtalen: ${avtale.utdanningslop?.utdanningsprogram?.navn}",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }
        val avtalensUtdanninger = avtale.utdanningslop?.utdanninger?.map { it.id } ?: emptyList()
        validate(avtalensUtdanninger.containsAll(utdanningslop.utdanninger)) {
            FieldError.of(
                "Lærefag må være valgt fra avtalens lærefag, minst ett av lærefagene mangler i avtalen.",
                GjennomforingDetaljerRequest::utdanningslop,
            )
        }

        utdanningslop
    }

    context(ctx: Context)
    private fun OpplaringKategoriseringRequest.toDbo(): AmoKategorisering {
        val kurstype = ctx.kurstyper.find { it.id == this.kurstypeId }
        val bransje = ctx.bransjer.find { it.id == this.bransjeId }
        val forerkort = (this.forerkort ?: emptySet()).mapNotNull { id -> ctx.forerkort.find { fk -> fk.id == id } }.toSet()
        val sertifiseringer = this.sertifiseringer?.toSet() ?: emptySet()
        val innholdsElementer = this.innholdElementer?.toSet() ?: emptySet()
        val norskprove = this.norskprove ?: false

        return when (kurstype?.kode) {
            Kurstype.Kode.BRANSJE_OG_YRKESRETTET -> AmoKategorisering(
                kurstype = kurstype,
                bransje = requireNotNull(bransje),
                sertifiseringer = sertifiseringer,
                innholdElementer = innholdsElementer,
                forerkort = forerkort,
                norskprove = false,
            )

            Kurstype.Kode.NORSKOPPLAERING -> AmoKategorisering(
                kurstype = kurstype,
                norskprove = norskprove,
                innholdElementer = innholdsElementer,
                bransje = null,
                forerkort = emptySet(),
                sertifiseringer = emptySet(),
            )

            Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> AmoKategorisering(
                kurstype = kurstype,
                innholdElementer = innholdsElementer,
                norskprove = false,
                bransje = null,
                forerkort = emptySet(),
                sertifiseringer = emptySet(),
            )

            Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> AmoKategorisering(
                kurstype = kurstype,
                innholdElementer = innholdsElementer,
                norskprove = false,
                bransje = null,
                forerkort = emptySet(),
                sertifiseringer = emptySet(),
            )

            Kurstype.Kode.STUDIESPESIALISERING -> AmoKategorisering(
                kurstype = kurstype,
                innholdElementer = emptySet(),
                norskprove = false,
                bransje = null,
                forerkort = emptySet(),
                sertifiseringer = emptySet(),
            )

            else -> throw IllegalArgumentException("Kurstype må være satt")
        }
    }
}
