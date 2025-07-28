package no.nav.mulighetsrommet.model

import kotlinx.serialization.Serializable

@Serializable
data class Faneinnhold(
    val forHvem: List<PortableTextTypedObject>? = null,
    val forHvemInfoboks: String? = null,
    val detaljerOgInnhold: List<PortableTextTypedObject>? = null,
    val detaljerOgInnholdInfoboks: String? = null,
    val pameldingOgVarighet: List<PortableTextTypedObject>? = null,
    val pameldingOgVarighetInfoboks: String? = null,
    val kontaktinfo: List<PortableTextTypedObject>? = null,
    val kontaktinfoInfoboks: String? = null,
    val oppskrift: List<PortableTextTypedObject>? = null,
    val lenker: List<FaneinnholdLenke>? = null,
    val delMedBruker: String? = null,
)

@Serializable
data class FaneinnholdLenke(
    val lenkenavn: String,
    val lenke: String,
    val apneINyFane: Boolean,
    val visKunForVeileder: Boolean,
)
