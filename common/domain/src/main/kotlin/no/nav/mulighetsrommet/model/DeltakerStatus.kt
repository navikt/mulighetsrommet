package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class DeltakerStatus(
    val type: DeltakerStatusType,
    val aarsak: DeltakerStatusAarsakType?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val opprettetDato: LocalDateTime,
)

enum class DeltakerStatusType(val description: String, val variant: DataElement.Status.Variant) {
    AVBRUTT("Avbrutt", DataElement.Status.Variant.NEUTRAL),
    AVBRUTT_UTKAST("Avbrutt utkast", DataElement.Status.Variant.NEUTRAL),
    DELTAR("Deltar", DataElement.Status.Variant.BLANK),
    FEILREGISTRERT("Feilregistrert", DataElement.Status.Variant.NEUTRAL),
    FULLFORT("Fullført", DataElement.Status.Variant.ALT_1),
    HAR_SLUTTET("Har sluttet", DataElement.Status.Variant.ALT_1),
    IKKE_AKTUELL("Ikke aktuell", DataElement.Status.Variant.NEUTRAL),
    KLADD("Kladd", DataElement.Status.Variant.WARNING),
    PABEGYNT_REGISTRERING("Påbegynt registrering", DataElement.Status.Variant.WARNING),
    SOKT_INN("Søkt inn", DataElement.Status.Variant.ALT_2),
    UTKAST_TIL_PAMELDING("Utkast til påmelding", DataElement.Status.Variant.INFO),
    VENTELISTE("Venteliste", DataElement.Status.Variant.ALT_1),
    VENTER_PA_OPPSTART("Venter på oppstart", DataElement.Status.Variant.ALT_3),
    VURDERES("Vurderes", DataElement.Status.Variant.ALT_2),
    ;

    fun toDataElement() = DataElement.Status(description, variant)
}

enum class DeltakerStatusAarsakType(val description: String) {
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
    KRAV_IKKE_OPPFYLT("Krav for deltakelse er ikke oppfylt"),
    KURS_FULLT("Kurset er fullt"),
}
