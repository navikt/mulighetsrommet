package no.nav.mulighetsrommet.api.domain

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.database.PGEnum
import no.nav.mulighetsrommet.api.domain.TiltaksgjennomforingTable.nullable
import no.nav.mulighetsrommet.api.utils.DateSerializer
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

enum class Tiltakskode {
    ABIST, ABOPPF, ABTBOPPF, ABUOPPF, AMBF1, AMBF2, AMBF3, AMO, AMOB, AMOE, AMOY, ANNUTDANN, ARBDOGNSM, ARBFORB, ARBRDAGSM, ARBRRDOGN, ARBRRHBAG, ARBRRHBSM, ARBRRHDAG, ARBTREN, ASV, ATG, AVKLARAG, AVKLARKV, AVKLARSP, AVKLARSV, AVKLARUS, BIA, BIO, BREVKURS, DIGIOPPARB, DIVTILT, EKSPEBIST, ENKELAMO, ENKFAGYRKE, ETAB, FLEKSJOBB, FORSAMOENK, FORSAMOGRU, FORSFAGENK, FORSFAGGRU, FORSHOYUTD, FUNKSJASS, GRUFAGYRKE, GRUNNSKOLE, GRUPPEAMO, HOYEREUTD, HOYSKOLE, INDJOBSTOT, INDOPPFAG, INDOPPFOLG, INDOPPFSP, INDOPPRF, INKLUTILS, INST_S, IPSUNG, ITGRTILS, JOBBBONUS, JOBBFOKUS, JOBBK, JOBBKLUBB, JOBBSKAP, KAT, KURS, LONNTIL, LONNTILAAP, LONNTILL, LONNTILS, MENTOR, MIDLONTIL, NETTAMO, NETTKURS, OPPLT2AAR, PRAKSKJERM, PRAKSORD, PV, REAKTUFOR, REFINO, SPA, STATLAERL, SUPPEMP, SYSSLANG, SYSSOFF, TIDSUBLONN, TILPERBED, TILRETTEL, TILRTILSK, TILSJOBB, UFØREPENLØ, UTBHLETTPS, UTBHPSLD, UTBHSAMLI, UTDPERMVIK, UTDYRK, UTVAOONAV, UTVOPPFOPL, VALS, VARLONTIL, VASV, VATIAROR, VIDRSKOLE, VIKARBLED, VV, YHEMMOFF
}

@Serializable
data class Tiltakstype(
    val id: Int? = null,
    val navn: String,
    val innsatsgruppe: Int,
    val sanityId: Int? = null,
    val tiltakskode: Tiltakskode,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val createdBy: String,
    @Serializable(with = DateSerializer::class)
    val createdAt: LocalDateTime
)

object TiltakstypeTable : IntIdTable() {
    val navn: Column<String> = text("navn")
    val innsatsgruppeId: Column<Int> = integer("innsatsgruppe_id").references(InnsatsgruppeTable.id)
    val sanityId: Column<Int?> = integer("sanity_id").nullable()
    val tiltakskode = customEnumeration("tiltakskode", "tiltakskode", { value -> Tiltakskode.valueOf(value as String) }, { PGEnum("tiltakskode", it) })
    val fraDato: Column<LocalDateTime?> = datetime("dato_fra").nullable()
    val tilDato: Column<LocalDateTime?> = datetime("dato_til").nullable()
    val createdBy: Column<String> = text("created_by")
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}
