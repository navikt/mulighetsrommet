package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggRequest
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class OpsjonLoggEntry(
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val sluttdato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val forrigeSluttdato: LocalDate?,
    val status: OpsjonLoggRequest.OpsjonsLoggStatus,
    val registrertAv: NavIdent,
)
