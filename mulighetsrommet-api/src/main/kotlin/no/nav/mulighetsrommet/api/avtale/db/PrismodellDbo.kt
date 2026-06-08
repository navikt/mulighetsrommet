package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class PrismodellDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: PrismodellType,
    val valuta: Valuta,
    val prisbetingelser: String? = null,
    val satser: List<AvtaltSats>? = null,
    val systemId: String? = null,
    val tilsagnPerDeltaker: Boolean? = null,
    val totalbelop: Int? = null,
    val tilskudd: Map<Opplaeringtilskudd.Kode, Int>? = null,
    val aarsak: String? = null,
)
