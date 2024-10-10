package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class DeltakerStatus(
    val type: Type,
    val aarsak: Aarsak?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetDato: LocalDateTime,
) {
    enum class Type(val description: String) {
        AVBRUTT("Avbrutt"),
        AVBRUTT_UTKAST("Avbrutt utkast"),
        DELTAR("Deltar"),
        FEILREGISTRERT("Feilregistrert"),
        FULLFORT("Fullført"),
        HAR_SLUTTET("Har sluttet"),
        IKKE_AKTUELL("Ikke aktuell"),
        KLADD("Kladd"),
        PABEGYNT_REGISTRERING("Påbegynt registrering"),
        SOKT_INN("Søkt om plass"),
        UTKAST_TIL_PAMELDING("Utkast til påmelding"),
        VENTELISTE("På venteliste"),
        VENTER_PA_OPPSTART("Venter på oppstart"),
        VURDERES("Vurderes"),
    }

    enum class Aarsak(val description: String) {
        ANNET("Annet"),
        AVLYST_KONTRAKT("Avlyst kontrakt"),
        FATT_JOBB("Fått jobb"),
        FEILREGISTRERT("Feilregistrert"),
        FERDIG("Ferdig"),
        FIKK_IKKE_PLASS("Fikk ikke plass"),
        IKKE_MOTT("Møter ikke opp"),
        OPPFYLLER_IKKE_KRAVENE("Oppfyller ikke kravene"),
        SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT("Samarbeidet med arrangøren er avbrutt"),
        SYK("Syk"),
        TRENGER_ANNEN_STOTTE("Trenger annen støtte"),
        UTDANNING("Utdanning"),
    }
}
