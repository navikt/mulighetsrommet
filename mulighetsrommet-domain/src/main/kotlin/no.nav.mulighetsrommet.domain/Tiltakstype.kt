package no.nav.mulighetsrommet.domain

import kotlinx.serialization.Serializable
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
    val createdBy: String? = "MRSYS",
    @Serializable(with = DateSerializer::class)
    val createdAt: LocalDateTime? = null,
    val updatedBy: String? = "MRSYS",
    @Serializable(with = DateSerializer::class)
    val updatedAt: LocalDateTime? = null
)
