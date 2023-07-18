package no.nav.mulighetsrommet.api.domain.int

import no.nav.mulighetsrommet.api.domain.dbo.AvtaleNotatDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingNotatDbo
import no.nav.mulighetsrommet.api.domain.dto.AvtaleNotatDto
import no.nav.mulighetsrommet.api.domain.dto.TiltaksgjennomforingNotatDto
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import no.nav.mulighetsrommet.api.utils.NotatFilter
import no.nav.mulighetsrommet.database.utils.QueryResult
import java.util.*

interface NotatService {
    fun getAllAvtaleNotater(filter: NotatFilter): StatusResponse<List<AvtaleNotatDto>>
    fun upsertAvtaleNotat(notat: AvtaleNotatDbo): StatusResponse<AvtaleNotatDto>
    fun getAvtaleNotat(id: UUID): QueryResult<AvtaleNotatDto?>

    fun deleteAvtaleNotat(id: UUID, navIdent: String): StatusResponse<Int>

    fun getAllTiltaksgjennomforingNotater(filter: NotatFilter): StatusResponse<List<TiltaksgjennomforingNotatDto>>
    fun upsertTiltaksgjennomforingNotat(notat: TiltaksgjennomforingNotatDbo): StatusResponse<TiltaksgjennomforingNotatDto>
    fun getTiltaksgjennomforingNotat(id: UUID): QueryResult<TiltaksgjennomforingNotatDto?>

    fun deleteTiltaksgjennomforingNotat(id: UUID, navIdent: String): StatusResponse<Int>
}
