package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.model.Periode
import no.nav.tiltak.okonomi.Tilskuddstype

sealed interface SystemgenerertPrismodell<B : UtbetalingBeregning> {
    val type: PrismodellType
    val tilskuddstype: Tilskuddstype

    fun justerPeriodeForBeregning(periode: Periode): Periode = periode

    interface FraDeltakelser<B : UtbetalingBeregning> : SystemgenerertPrismodell<B> {
        fun beregn(gjennomforing: GjennomforingAvtale, periode: Periode, deltakere: List<Deltaker>): B
    }

    interface FraTilsagn<B : UtbetalingBeregning> : SystemgenerertPrismodell<B> {
        fun beregn(gjennomforing: GjennomforingAvtale, periode: Periode, tilsagn: List<Tilsagn>): B
    }
}
