@file:UseSerializers(UUIDSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.mulighetsrommet.api.domain.tiltak

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Avtale(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val navn: String,
    val avtalenummer: String?,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: Arrangor?,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val avtaletype: Avtaletype,
    val status: AvtaleStatus,
    val administratorer: Set<NavIdent>,
    val veilederinfo: VeilederInfo,
    val personvern: Personvern,
    val opplaring: OpplaringKategorisering?,
    val opsjoner: Opsjoner,
    val prisinfo: Prisinfo,
) {
    @Serializable
    data class Arrangor(
        val hovedenhet: UUID,
        val underenheter: List<UUID>,
        val kontaktpersoner: List<UUID> = emptyList(),
    )

    /**
     * Skiller mellom prismodeller som avtalen selv eier ([Egendefinert]) og en prismodell som er eid av systemet
     * og delt av alle avtaler for en gitt tiltakskode ([Systembestemt], for forhåndsgodkjente avtaler).
     */
    @Serializable
    sealed interface Prisinfo {
        @Serializable
        data class Egendefinert(val prismodeller: List<Prismodell>) : Prisinfo

        @Serializable
        data class Systembestemt(val prismodell: Prismodell) : Prisinfo

        fun toList(): List<Prismodell> = when (this) {
            is Egendefinert -> prismodeller
            is Systembestemt -> listOf(prismodell)
        }
    }

    @Serializable
    data class VeilederInfo(
        val beskrivelse: String? = null,
        val faneinnhold: Faneinnhold? = null,
        val navEnheter: Set<NavEnhetNummer> = setOf(),
    )

    @Serializable
    data class Personvern(
        val personopplysninger: Set<Personopplysning.Type>,
        val annetBeskrivelse: String?,
        val erBekreftet: Boolean,
    ) {
        companion object {
            fun bekreftet(
                personopplysninger: Set<Personopplysning.Type> = setOf(),
                annetBeskrivelse: String? = null,
            ): Personvern {
                return Personvern(
                    personopplysninger = personopplysninger,
                    annetBeskrivelse = annetBeskrivelse,
                    erBekreftet = true,
                )
            }
        }
    }

    @Serializable
    data class Opsjoner(
        val modell: Opsjonsmodell,
        val registreringer: List<OpsjonLogg>,
    )

    @Serializable
    data class OpsjonLogg(
        val id: UUID,
        val createdAt: LocalDateTime,
        val sluttDato: LocalDate?,
        val forrigeSluttDato: LocalDate,
        val status: OpsjonLoggStatus,
    )
}
