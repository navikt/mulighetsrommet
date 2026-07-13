package no.nav.mulighetsrommet.admin.totrinnskontroll

import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import java.util.UUID

interface TotrinnskontrollQueryHandler {
    fun getDto(entityId: UUID, type: TotrinnskontrollType): TotrinnskontrollDto?

    fun getDtoOrError(entityId: UUID, type: TotrinnskontrollType): TotrinnskontrollDto

    // TODO: saneres etter at Aggregate-modeller som er avhengig av totrinnskontroll har blitt flyttet til domain
    fun upsert(totrinnskontroll: Totrinnskontroll)
    fun get(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll?
    fun getOrError(entityId: UUID, type: TotrinnskontrollType): Totrinnskontroll
}
