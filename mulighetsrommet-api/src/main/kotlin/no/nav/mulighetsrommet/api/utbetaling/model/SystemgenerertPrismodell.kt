package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.avtale.model.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype

interface SystemgenerertPrismodell<B : UtbetalingBeregning> {
    val type: PrismodellType
    val tilskuddstype: Tilskuddstype

    fun justerPeriodeForBeregning(periode: Periode): Periode = periode

    fun beregn(gjennomforing: GjennomforingAvtale, deltakere: List<Deltaker>, periode: Periode): B
}
