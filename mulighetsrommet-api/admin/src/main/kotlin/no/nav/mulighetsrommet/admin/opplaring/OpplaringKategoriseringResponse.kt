package no.nav.mulighetsrommet.admin.opplaring

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

/**
 * Representerer kodeverket knyttet til en bestemt [Tiltakskode].
 *
 * Kodeverket består av et hierarki av [Alternativ]-er som beskriver de relaterte
 * valgene som er aktuelle for tiltaket — for eksempel bransjer, førerkortklasser
 * eller sertifiseringer.
 *
 * Hierarkiet kan inneholde flere nivåer: rene grupperingsnoder ([Alternativ.Gruppe])
 * for å organisere innholdet, og innerste valgbare grupper ([Alternativ.Verdigruppe])
 * som inneholder de konkrete [Alternativ.Verdi]-ene brukeren kan velge mellom.
 *
 * @property tiltakskode Tiltakskoden kodeverket gjelder for.
 * @property alternativer Toppnivå-containere i kodeverket — enten [Alternativ.Gruppe]
 *   eller [Alternativ.Verdigruppe]. Kan ikke inneholde [Alternativ.Verdi] direkte.
 */
@Serializable
data class OpplaringKategoriseringResponse(
    val tiltakskode: Tiltakskode,
    val alternativer: List<Alternativ.Container>,
) {
    /**
     * Angir hvordan brukeren kan velge blant verdiene i en [Alternativ.Verdigruppe].
     */

    @Serializable
    enum class Seleksjonstype {
        /** Brukeren kan velge nøyaktig én verdi. */
        ENKELTVALG,

        /** Brukeren kan velge flere verdier samtidig. */
        FLERVALG,
    }

    @Serializable
    enum class Representerer {
        KURSTYPE_ID,
        BRANSJE_ID,
        SERTIFISERINGER,
        FORERKORT,
        INNHOLDSELEMENTER,
        NORSKPROVE,
        UTDANNINGSPROGRAM_ID,
        LAREFAG,
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator("type")
    sealed interface Tooltip {

        @Serializable
        @SerialName("Utlisting")
        data class Utlisting(
            val tittel: String,
            val innhold: List<String>,
        ) : Tooltip

        @Serializable
        @SerialName("FlereUtlistinger")
        data class FlereUtlistinger(
            val liste: List<Utlisting>,
        ) : Tooltip
    }

    /**
     * Et element i kodeverk-hierarkiet.
     *
     * Et alternativ er enten en [Container] som inneholder andre alternativer
     * ([Gruppe] eller [Verdigruppe]), eller en konkret valgbar [Verdi].
     *
     * @property id Unik identifikator for alternativet.
     * @property visningsnavn Navnet som vises i UI.
     */

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonClassDiscriminator("type")
    sealed interface Alternativ {
        val id: UUID?
        val visningsnavn: String

        /**
         * Et alternativ som kan inneholde andre alternativer — enten en ren
         * grupperingsnode ([Gruppe]) eller en valgbar gruppe med [Verdi]-er
         * ([Verdigruppe]).
         *
         * En [Verdi] er ikke en [Container] og kan derfor ikke inneholde andre
         * alternativer.
         */
        @Serializable
        @SerialName("Container")
        sealed interface Container : Alternativ

        /**
         * En ren grupperingsnode som inneholder andre [Container]-er.
         *
         * Brukes for å støtte mer enn to nivåer i hierarkiet — f.eks. en
         * overordnet kategori som inneholder flere [Verdigruppe]-r eller
         * nestede [Gruppe]-r. En [Gruppe] har ingen [Seleksjonstype] siden
         * brukeren ikke velger direkte fra den; valgene skjer i de underliggende
         * [Verdigruppe]-ene.
         *
         * @property id Unik identifikator for gruppen.
         * @property visningsnavn Navnet som vises i UI.
         * @property alternativer Underliggende containere — kan ikke inneholde
         *   [Verdi]-er direkte.
         */
        @Serializable
        @SerialName("Gruppe")
        data class Gruppe(
            @Serializable(with = UUIDSerializer::class)
            override val id: UUID?,
            override val visningsnavn: String,
            val representerer: Representerer?,
            val pakrevd: Boolean,
            val alternativer: List<Container>,
        ) : Container

        /**
         * Gruppering for utdanningsprogram og lærefag
         *
         * Muliggjør at valg av program, gir andre muligheter for lærefag
         */
        @Serializable
        @SerialName("UtdanningGruppe")
        data class UtdanningGruppe(
            @Serializable(with = UUIDSerializer::class)
            override val id: UUID? = null,
            override val visningsnavn: String,
            val representerer: Representerer,
            val pakrevd: Boolean,
            val utdanninger: List<UtdanningValg>,
        ) : Container {
            @Serializable
            data class UtdanningValg(
                @Serializable(with = UUIDSerializer::class)
                val id: UUID,
                val visningsnavn: String,
                val larefag: Verdigruppe,
            )
        }

        /**
         * En valgbar gruppe — det innerste nivået i hierarkiet som inneholder
         * direkte valgbare [Verdi]-er.
         *
         * Eksempler på verdigrupper:
         * - "Bransje" med verdier "Bygg og anlegg", "Helse og omsorg"
         * - "Førerkortklasse" med verdier "B", "C1", "CE"
         *
         * @property id Unik identifikator for verdigruppen.
         * @property visningsnavn Navnet som vises i UI (f.eks. "Bransje").
         * @property seleksjonstype Hvordan brukeren kan velge blant verdiene
         *   (ett enkelt valg eller flere samtidig).
         * @property alternativer Verdiene brukeren kan velge mellom.
         */
        @Serializable
        @SerialName("Verdigruppe")
        data class Verdigruppe(
            @Serializable(with = UUIDSerializer::class)
            override val id: UUID?,
            override val visningsnavn: String,
            val tooltip: Tooltip?,
            val pakrevd: Boolean,
            val representerer: Representerer,
            val seleksjonstype: Seleksjonstype,
            val alternativer: List<Verdi>,
        ) : Container

        /**
         * Representerer en Verdigruppe, hvor verdiene må søkes etter i en gitt kilde
         * Siden søk er mer omfattende, og har sin egen responsstruktur, dekkes ikke integrasjonsdetaljene her
         * Eksempler på VerdigruppeSok:
         *  - Janzz sertifisering
         *
         * @property id Unik identifikator for verdigruppen.
         * @property visningsnavn Navnet som vises i UI (f.eks. "Sertifiseringer").
         * @property seleksjonstype Hvordan brukeren kan velge blant verdiene
         *   (ett enkelt valg eller flere samtidig).
         * @property kilde opphavet til verdiene som kan velges
         */
        @Serializable
        @SerialName("VerdigruppeSok")
        data class VerdigruppeSok(
            @Serializable(with = UUIDSerializer::class)
            override val id: UUID?,
            override val visningsnavn: String,
            val pakrevd: Boolean,
            val representerer: Representerer,
            val seleksjonstype: Seleksjonstype,
            val kilde: Kilde,
        ) : Container {
            enum class Kilde {
                JANZZ_SERTIFISERING,
            }
        }

        /**
         * En konkret valgbar verdi i en [Verdigruppe].
         *
         * Eksempler på verdier:
         * - Bransje: "Bygg og anlegg", "Helse og omsorg"
         * - Førerkortklasse: "B", "C1", "CE"
         * - Sertifisering: "Truckførerbevis", "Varmt arbeid"
         *
         * @property id Unik identifikator for verdien.
         * @property visningsnavn Navnet som vises i UI.
         * @property valgt Om verdien er aktivert for denne gjennomføringen. Dette feltet
         *   er internt hos Komet og er ikke en del av selve kodeverket.
         */
        @Serializable
        @SerialName("Verdi")
        data class Verdi(
            @Serializable(with = UUIDSerializer::class)
            override val id: UUID,
            override val visningsnavn: String,
        ) : Alternativ
    }
}
