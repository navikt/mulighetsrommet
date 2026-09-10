@file:UseSerializers(UUIDSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)
@file:OptIn(ExperimentalContracts::class)

package no.nav.mulighetsrommet.api.domain.avtale

import arrow.core.Either
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodeller
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.validation.validation
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.contracts.ExperimentalContracts

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
    val rammedetaljer: Rammedetaljer? = null,
) {
    fun medRammedetaljer(totalRamme: Long?, utbetaltArena: Long?): Either<List<FieldError>, Avtale> = validation {
        validate(prisinfo !is Prisinfo.Systembestemt) {
            FieldError.of(
                "Rammedetaljer kan kun legges til anskaffet avtaler",
                Rammedetaljer::totalRamme,
            )
        }

        val prismodeller = prisinfo.toList()
        validate(prismodeller.distinctBy { it.valuta }.count() == 1) {
            FieldError.of(
                "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                Rammedetaljer::totalRamme,
            )
        }
        totalRamme?.let {
            validate(it > 0) {
                FieldError.of(
                    "Total ramme må være et positivt beløp",
                    Rammedetaljer::totalRamme,
                )
            }
        }
        utbetaltArena?.let {
            validate(it >= 0) {
                FieldError.of(
                    "Utbetalt beløp fra Arena må være et positivt beløp",
                    Rammedetaljer::utbetaltArena,
                )
            }
        }

        copy(
            rammedetaljer = Rammedetaljer(
                totalRamme = totalRamme,
                utbetaltArena = utbetaltArena,
                valuta = prismodeller.first().valuta,
            ),
        )
    }

    fun slettRammedetaljer(): Avtale = copy(rammedetaljer = null)

    fun medPrismodeller(prismodeller: List<Prismodell>): Either<List<FieldError>, Avtale> = validation {
        requireValid(avtaletype != Avtaletype.FORHANDSGODKJENT) {
            FieldError.of("Prismodell kan ikke endres for forhåndsgodkjente avtaler")
        }

        val prisinfo = Prisinfo.Egendefinert.of(tiltakskode, prismodeller).bind()

        copy(prisinfo = prisinfo)
    }

    fun medPersonvern(personvern: Personvern): Avtale = copy(personvern = personvern)

    fun medVeilederinfo(veilederinfo: VeilederInfo): Avtale = copy(veilederinfo = veilederinfo)

    fun avslutt(avsluttetTidspunkt: LocalDateTime): Either<List<FieldError>, Avtale> = validation {
        validate(status == AvtaleStatus.Aktiv) {
            FieldError.of("Avtalen må være aktiv for å kunne avsluttes")
        }

        val tidspunktForSlutt = sluttDato?.plusDays(1)?.atStartOfDay()
        validate(tidspunktForSlutt != null && !avsluttetTidspunkt.isBefore(tidspunktForSlutt)) {
            FieldError.of("Avtalen kan ikke avsluttes før sluttdato")
        }

        copy(status = AvtaleStatus.Avsluttet)
    }

    fun avbryt(
        tidspunkt: LocalDateTime,
        aarsaker: List<AvbrytAvtaleAarsak>,
        forklaring: String?,
    ): Either<List<FieldError>, Avtale> = validation {
        when (status) {
            is AvtaleStatus.Utkast, is AvtaleStatus.Aktiv -> Unit
            is AvtaleStatus.Avbrutt -> error { FieldError.of("Avtalen er allerede avbrutt") }
            is AvtaleStatus.Avsluttet -> error { FieldError.of("Avtalen er allerede avsluttet") }
        }

        copy(status = AvtaleStatus.Avbrutt(tidspunkt, aarsaker, forklaring))
    }

    fun oppdaterVarighet(nySluttDato: LocalDate, today: LocalDate): Avtale {
        val nyStatus = when (status) {
            is AvtaleStatus.Utkast, is AvtaleStatus.Avbrutt -> status

            is AvtaleStatus.Aktiv, is AvtaleStatus.Avsluttet -> if (!nySluttDato.isBefore(today)) {
                AvtaleStatus.Aktiv
            } else {
                AvtaleStatus.Avsluttet
            }
        }

        return copy(sluttDato = nySluttDato, status = nyStatus)
    }

    @Serializable
    data class Rammedetaljer(
        val totalRamme: Long?,
        val utbetaltArena: Long?,
        val valuta: Valuta,
    )

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
        data class Egendefinert(val prismodeller: List<Prismodell>) : Prisinfo {
            companion object {
                fun of(
                    tiltakskode: Tiltakskode,
                    prismodeller: List<Prismodell>,
                ): Either<List<FieldError>, Egendefinert> = validation {
                    requireValid(prismodeller.isNotEmpty()) {
                        FieldError("/prismodeller", "Minst én prismodell er påkrevd")
                    }

                    prismodeller.forEachIndexed { index, prismodell ->
                        validate(prismodell.type in Prismodeller.getPrismodellerForTiltak(tiltakskode)) {
                            FieldError(
                                "/prismodeller/$index/type",
                                "${prismodell.type.navn} er ikke tillatt for tiltakskode ${tiltakskode.name}",
                            )
                        }
                        validate(prismodell.type != PrismodellType.FAST_SATS_PER_BENYTTET_PLASS_PER_MANED) {
                            FieldError(
                                "/prismodeller",
                                "Prismodell kan ikke opprettes med typen ${prismodell.type.navn}",
                            )
                        }
                    }

                    Egendefinert(prismodeller)
                }
            }
        }

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
    ) {
        companion object {
            fun of(
                beskrivelse: String?,
                faneinnhold: Faneinnhold?,
                navEnheter: Set<NavEnhet>,
            ): Either<List<FieldError>, VeilederInfo> = validation {
                val regioner = navEnheter.filter { it.type == NavEnhetType.FYLKE }.map { it.enhetsnummer }.toSet()
                validate(regioner.isNotEmpty()) {
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region")
                }

                val kontorer = navEnheter.filter { it.overordnetEnhet in regioner }.map { it.enhetsnummer }.toSet()
                validate(kontorer.isNotEmpty()) {
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet")
                }

                VeilederInfo(
                    beskrivelse = beskrivelse,
                    faneinnhold = faneinnhold,
                    navEnheter = regioner + kontorer,
                )
            }
        }
    }

    @Serializable
    data class Personvern(
        val personopplysninger: Set<Personopplysning.Type>,
        val annetBeskrivelse: String?,
        val erBekreftet: Boolean,
    ) {
        companion object {
            fun of(
                personopplysninger: Set<Personopplysning.Type>,
                annetBeskrivelse: String?,
                erBekreftet: Boolean,
            ): Either<List<FieldError>, Personvern> = validation {
                requireValid(Personopplysning.Type.ANNET !in personopplysninger || !annetBeskrivelse.isNullOrBlank()) {
                    FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")
                }
                requireValid((annetBeskrivelse?.length ?: 0) <= 300) {
                    FieldError("/personvern/annetBeskrivelse", "Beskrivelse kan maks være 300 tegn")
                }

                Personvern(
                    personopplysninger = personopplysninger,
                    annetBeskrivelse = annetBeskrivelse?.takeIf { Personopplysning.Type.ANNET in personopplysninger },
                    erBekreftet = erBekreftet,
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
