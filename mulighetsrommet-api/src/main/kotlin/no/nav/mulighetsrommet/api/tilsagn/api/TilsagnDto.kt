package no.nav.mulighetsrommet.api.tilsagn.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.navenhet.NavEnhetDto
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaMedGeografiskEnhet
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TilsagnDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: TilsagnType,
    val periode: Periode,
    val pris: ValutaBelop,
    val belopBrukt: ValutaBelop,
    val belopGjenstaende: ValutaBelop,
    val kostnadssted: KostnadsstedDto,
    val bestillingsnummer: String,
    val status: TilsagnStatusDto,
    val kommentar: String?,
    val beskrivelse: String?,
    val deltakere: List<TilsagnDeltakerDto>,
) {
    companion object {
        fun from(tilsagn: Tilsagn, deltakere: List<TilsagnDeltakerDto>) = TilsagnDto(
            id = tilsagn.id,
            type = tilsagn.type,
            periode = tilsagn.periode,
            pris = tilsagn.beregning.output.pris,
            belopBrukt = tilsagn.belopBrukt,
            belopGjenstaende = tilsagn.gjenstaendeBelop(),
            kostnadssted = KostnadsstedDto.fromNavEnhetDbo(tilsagn.kostnadssted),
            bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
            status = TilsagnStatusDto(tilsagn.status),
            kommentar = tilsagn.kommentar,
            beskrivelse = tilsagn.beskrivelse,
            deltakere = deltakere,
        )
    }
}

@Serializable
data class TilsagnStatusDto(
    val type: TilsagnStatus,
) {
    val status: DataElement.Status = toTilsagnStatusTag(type)
}

@Serializable
data class TilsagnDeltakerDto(
    @Serializable(with = UUIDSerializer::class)
    val deltakerId: UUID,
    val norskIdent: NorskIdent?,
    val navn: String,
    val oppfolgingEnhet: NavEnhetDto?,
    val geografiskEnhet: NavEnhetDto?,
    val innholdAnnet: String?,
    val status: DataElement.Status,
) {
    companion object {
        fun from(
            deltaker: Tilsagn.Deltaker,
            personalia: PersonaliaMedGeografiskEnhet?,
        ) = TilsagnDeltakerDto(
            deltakerId = deltaker.deltakerId,
            norskIdent = personalia?.norskIdent,
            navn = personalia?.navn ?: "Ukjent",
            oppfolgingEnhet = personalia?.oppfolgingEnhet,
            geografiskEnhet = personalia?.geografiskEnhet,
            innholdAnnet = deltaker.innholdAnnet,
            status = deltaker.status.toDataElement(),
        )

        fun from(deltaker: Deltaker, personalia: PersonaliaMedGeografiskEnhet?) = TilsagnDeltakerDto(
            deltakerId = deltaker.id,
            norskIdent = personalia?.norskIdent,
            navn = personalia?.navn ?: "Ukjent",
            oppfolgingEnhet = personalia?.oppfolgingEnhet,
            geografiskEnhet = personalia?.geografiskEnhet,
            innholdAnnet = deltaker.innholdAnnet,
            status = deltaker.status.type.toDataElement(),
        )
    }
}
