@file:UseSerializers(
    UUIDSerializer::class,
    LocalDateSerializer::class,
    LocalDateTimeSerializer::class,
    InstantSerializer::class,
)

package no.nav.mulighetsrommet.api.domain.avtale

import arrow.core.Either
import arrow.core.right
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
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.validation.FieldError
import no.nav.mulighetsrommet.validation.FieldValidator
import no.nav.mulighetsrommet.validation.validation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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
    val rammedetaljer: Rammedetaljer? = null,
) {
    companion object {
        fun opprett(
            command: OpprettAvtaleCommand,
            vurderingsdato: Vurderingsdato,
        ): Either<List<FieldError>, Avtale> = validation {
            validateAvtaletypeOgOpsjonsmodell(command.avtaletype, command.opsjonsmodell)

            val sluttDato = validateVarighet(command.opsjonsmodell, command.startDato, command.sluttDato)

            val status = if (command.arrangor == null) {
                AvtaleStatus.Utkast
            } else {
                statusFraSluttDato(sluttDato, vurderingsdato)
            }

            Avtale(
                id = command.id,
                tiltakskode = command.tiltakskode,
                navn = command.navn,
                avtalenummer = null,
                sakarkivNummer = command.sakarkivNummer,
                arrangor = command.arrangor,
                startDato = command.startDato,
                sluttDato = sluttDato,
                avtaletype = command.avtaletype,
                status = status,
                administratorer = command.administratorer,
                veilederinfo = command.veilederinfo,
                personvern = command.personvern,
                opplaring = command.opplaring,
                opsjoner = Opsjoner(command.opsjonsmodell, emptyList()),
                prisinfo = command.prisinfo,
            )
        }
    }

    fun oppdaterDetaljer(
        command: OppdaterAvtaledetaljerCommand,
        vurderingsdato: Vurderingsdato,
    ): Either<List<FieldError>, Avtale> = validation {
        validateAvtaletypeOgOpsjonsmodell(command.avtaletype, command.opsjonsmodell)

        val harRegistrerteOpsjoner = opsjoner.registreringer.isNotEmpty()

        validate(!harRegistrerteOpsjoner || command.avtaletype == avtaletype) {
            FieldError.of("Du kan ikke endre avtaletype når opsjoner er registrert", Avtale::avtaletype)
        }
        validate(!harRegistrerteOpsjoner || command.startDato == startDato) {
            FieldError.of("Startdato kan ikke endres når opsjoner er registrert", Avtale::startDato)
        }
        validate(!harRegistrerteOpsjoner || command.opsjonsmodell == opsjoner.modell) {
            FieldError.of("Opsjonsmodell kan ikke endres når opsjoner er registrert", Avtale::opsjoner)
        }
        validate(!harRegistrerteOpsjoner || command.sluttDato == sluttDato) {
            FieldError.of("Sluttdato kan ikke endres når opsjoner er registrert", Avtale::sluttDato)
        }

        val sluttDato = if (harRegistrerteOpsjoner) {
            sluttDato
        } else {
            validateVarighet(command.opsjonsmodell, command.startDato, command.sluttDato)
        }
        val status = when {
            command.arrangor == null -> AvtaleStatus.Utkast
            status is AvtaleStatus.Avbrutt -> status
            else -> statusFraSluttDato(sluttDato, vurderingsdato)
        }

        copy(
            navn = command.navn,
            sakarkivNummer = command.sakarkivNummer,
            arrangor = command.arrangor,
            startDato = command.startDato,
            sluttDato = sluttDato,
            avtaletype = command.avtaletype,
            status = status,
            administratorer = command.administratorer,
            opplaring = command.opplaring,
            opsjoner = opsjoner.copy(modell = command.opsjonsmodell),
        )
    }

    fun medRammedetaljer(totalRamme: Long?, utbetaltArena: Long?): Either<List<FieldError>, Avtale> {
        if (totalRamme == null && utbetaltArena == null) {
            // Fjerner rammedetaljer uten videre validering - tilsvarer en sletting.
            return copy(rammedetaljer = null).right()
        }

        return validation {
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
    }

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
        validate(tidspunktForSlutt != null && tidspunktForSlutt <= avsluttetTidspunkt) {
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

    fun registrerOpsjon(
        command: RegistrerOpsjonCommand,
        vurderingsdato: Vurderingsdato,
        id: UUID = UUID.randomUUID(),
        registrertTidspunkt: Instant = instantAsMicros(),
    ): Either<List<FieldError>, Avtale> = validation {
        val modell = opsjoner.modell
        requireValid(modell is Opsjonsmodell.MedOpsjonsmulighet) {
            FieldError.of("Opsjon kan ikke registreres for denne opsjonsmodellen")
        }

        val gjeldendeSluttDato = requireNotNull(sluttDato)

        validate(
            command !is RegistrerOpsjonCommand.UtloseOpsjon || command.nySluttDato > gjeldendeSluttDato,
        ) {
            FieldError.of("Ny sluttdato må være etter gjeldende sluttdato")
        }
        validate(
            command !is RegistrerOpsjonCommand.UtloseOpsjon || command.nySluttDato <= modell.maksSluttDato(startDato),
        ) {
            FieldError.of("Ny sluttdato er forbi maks varighet av avtalen")
        }

        validate(opsjoner.registreringer.none { it is Opsjon.SkalIkkeUtloseOpsjon }) {
            FieldError.of("Kan ikke utløse flere opsjoner")
        }

        val opsjon = when (command) {
            is RegistrerOpsjonCommand.UtloseOpsjon -> Opsjon.OpsjonUtlost(
                id = id,
                registrertTidspunkt = registrertTidspunkt,
                forrigeSluttDato = gjeldendeSluttDato,
                registrertAv = command.registrertAv,
                nySluttDato = command.nySluttDato,
            )

            is RegistrerOpsjonCommand.SkalIkkeUtloseOpsjon -> Opsjon.SkalIkkeUtloseOpsjon(
                id = id,
                registrertTidspunkt = registrertTidspunkt,
                forrigeSluttDato = gjeldendeSluttDato,
                registrertAv = command.registrertAv,
            )
        }

        val medOpsjon = copy(opsjoner = opsjoner.copy(registreringer = opsjoner.registreringer + opsjon))

        when (command) {
            is RegistrerOpsjonCommand.UtloseOpsjon -> medOpsjon.oppdaterVarighet(command.nySluttDato, vurderingsdato)
            is RegistrerOpsjonCommand.SkalIkkeUtloseOpsjon -> medOpsjon
        }
    }

    fun slettOpsjon(
        opsjonId: UUID,
        vurderingsdato: Vurderingsdato,
    ): Either<List<FieldError>, Avtale> = validation {
        val sisteOpsjon = opsjoner.registreringer.lastOrNull()

        requireValid(sisteOpsjon != null && sisteOpsjon.id == opsjonId) {
            FieldError.of("Opsjonen kan ikke slettes fordi det ikke er den siste utløste opsjonen")
        }

        val gjenvaerendeRegistreringer = opsjoner.registreringer.filterNot { it.id == opsjonId }
        val medOppdatertLogg = copy(opsjoner = opsjoner.copy(registreringer = gjenvaerendeRegistreringer))

        when (sisteOpsjon) {
            is Opsjon.OpsjonUtlost -> medOppdatertLogg.oppdaterVarighet(sisteOpsjon.forrigeSluttDato, vurderingsdato)
            is Opsjon.SkalIkkeUtloseOpsjon -> medOppdatertLogg
        }
    }

    internal fun oppdaterVarighet(
        nySluttDato: LocalDate,
        vurderingsdato: Vurderingsdato,
    ): Avtale {
        val nyStatus = when (status) {
            is AvtaleStatus.Utkast, is AvtaleStatus.Avbrutt -> status
            is AvtaleStatus.Aktiv, is AvtaleStatus.Avsluttet -> statusFraSluttDato(nySluttDato, vurderingsdato)
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
        val registreringer: List<Opsjon>,
    )

    @Serializable
    sealed interface Opsjon {
        val id: UUID
        val registrertTidspunkt: Instant
        val forrigeSluttDato: LocalDate
        val registrertAv: NavIdent

        @Serializable
        data class OpsjonUtlost(
            override val id: UUID,
            override val registrertTidspunkt: Instant,
            override val forrigeSluttDato: LocalDate,
            override val registrertAv: NavIdent,
            val nySluttDato: LocalDate,
        ) : Opsjon

        @Serializable
        data class SkalIkkeUtloseOpsjon(
            override val id: UUID,
            override val registrertTidspunkt: Instant,
            override val forrigeSluttDato: LocalDate,
            override val registrertAv: NavIdent,
        ) : Opsjon
    }
}

private fun instantAsMicros(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

private fun statusFraSluttDato(sluttDato: LocalDate?, vurderingsdato: Vurderingsdato): AvtaleStatus {
    return if (sluttDato == null || vurderingsdato.value <= sluttDato) {
        AvtaleStatus.Aktiv
    } else {
        AvtaleStatus.Avsluttet
    }
}

private fun FieldValidator.validateAvtaletypeOgOpsjonsmodell(avtaletype: Avtaletype, opsjonsmodell: Opsjonsmodell) {
    validate(avtaletype != Avtaletype.FORHANDSGODKJENT || opsjonsmodell is Opsjonsmodell.ValgfriSluttdato) {
        FieldError.of(
            "Du må velge opsjonsmodell med valgfri sluttdato når avtalen er forhåndsgodkjent",
            Avtale::opsjoner,
        )
    }
}

private fun FieldValidator.validateVarighet(
    opsjonsmodell: Opsjonsmodell,
    startDato: LocalDate,
    sluttDato: LocalDate?,
): LocalDate? {
    val gjeldendeSluttDato = when (opsjonsmodell) {
        Opsjonsmodell.ToPlussEn,
        Opsjonsmodell.ToPlussEnPlussEn,
        Opsjonsmodell.ToPlussEnPlussEnPlussEn,
        -> startDato.plusYears(2).minusDays(1)

        is Opsjonsmodell.Annet, Opsjonsmodell.IngenOpsjonsmulighet -> {
            requireNotNull(sluttDato) {
                FieldError.of("Du må legge inn sluttdato for avtalen", Avtale::sluttDato)
            }
            sluttDato
        }

        Opsjonsmodell.ValgfriSluttdato -> sluttDato
    }
    validate(gjeldendeSluttDato == null || startDato <= gjeldendeSluttDato) {
        FieldError.of("Startdato må være før sluttdato", Avtale::startDato)
    }
    if (opsjonsmodell is Opsjonsmodell.MedOpsjonsmulighet) {
        validate(gjeldendeSluttDato != null && gjeldendeSluttDato <= opsjonsmodell.maksSluttDato(startDato)) {
            FieldError.of("Sluttdato er forbi maks varighet av avtalen", Avtale::sluttDato)
        }
    }
    return gjeldendeSluttDato
}
