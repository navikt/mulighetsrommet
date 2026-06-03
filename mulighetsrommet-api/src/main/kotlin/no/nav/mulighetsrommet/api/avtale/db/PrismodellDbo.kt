package no.nav.mulighetsrommet.api.avtale.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.util.UUID

@Serializable
data class PrismodellDbo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: PrismodellType,
    val valuta: Valuta,
    val prisbetingelser: String?,
    val satser: List<AvtaltSats>?,
    val systemId: String?,
    val tilsagnPerDeltaker: Boolean?,
    val totalbelop: UInt? = null,
    val tilskudd: Map<Tilskuddstype, UInt>? = null,
)
