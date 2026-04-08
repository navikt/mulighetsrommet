package no.nav.mulighetsrommet.api.arrangorflate.model

import kotliquery.Row
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.datatypes.periode
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.util.UUID

data class ArrangorflateTilsagnKompakt(
    val id: UUID,
    val periode: Periode,
    val gjennomforing: Gjennomforing,
    val arrangor: Arrangor,
    val tiltakstype: Tiltakstype,
    val type: TilsagnType,
    val status: TilsagnStatus,
    val bestillingsnummer: String,
) {
    data class Arrangor(
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
    )

    data class Gjennomforing(
        val navn: String,
        val lopenummer: Tiltaksnummer,
    )

    data class Tiltakstype(
        val navn: String,
        val tiltakskode: Tiltakskode,
    )
}

fun Row.toArrangorflateTilsagnKompakt(): ArrangorflateTilsagnKompakt = ArrangorflateTilsagnKompakt(
    id = uuid("id"),
    type = TilsagnType.valueOf(string("tilsagn_type")),
    tiltakstype = ArrangorflateTilsagnKompakt.Tiltakstype(
        tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
        navn = string("tiltakstype_navn"),
    ),
    gjennomforing = ArrangorflateTilsagnKompakt.Gjennomforing(
        lopenummer = Tiltaksnummer(string("gjennomforing_lopenummer")),
        navn = string("gjennomforing_navn"),
    ),
    periode = periode("periode"),
    bestillingsnummer = string("bestillingsnummer"),
    arrangor = ArrangorflateTilsagnKompakt.Arrangor(
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        navn = string("arrangor_navn"),
    ),
    status = TilsagnStatus.valueOf(string("status")),
)
