package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.admin.totrinnskontroll.AgentDto
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.pdfgen.Deltaker
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.Signature
import no.nav.mulighetsrommet.api.pdfgen.TopSection
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import java.time.LocalDateTime

object TilskuddVedtakToPdfDocumentContentMapper {
    fun toPdfDocumentContent(
        tilskuddBehandling: TilskuddBehandling,
        navn: String,
        norskIdent: NorskIdent?,
        gjennomforing: GjennomforingEnkeltplass,
        saksbehandler: AgentDto,
        beslutter: AgentDto,
        besluttetTidspunkt: LocalDateTime,
    ): PdfDocumentContent {
        val saksnummer = gjennomforing.lopenummer.value

        return PdfDocumentContent.create(
            title = "Vedtak om tilskudd til opplæring – $navn ($saksnummer)",
            subject = "Vedtak om tilskudd til opplæring",
            description = "Vedtak om tilskudd til opplæring til $navn",
            author = "Nav",
        ) {
            topSection(
                TopSection(
                    reference = saksnummer,
                    date = besluttetTidspunkt.toLocalDate().toString(),
                    deltaker = Deltaker(
                        navn = navn,
                        norskIdent = norskIdent?.value,
                    ),
                ),
            )

            mainSection("Vedtak om tilskudd til opplæring")

            tilskuddBehandling.tilskudd.forEach { tilskudd ->
                when (tilskudd.vedtakResultat) {
                    VedtakResultat.INNVILGELSE -> innvilgelseSection(tilskudd, tilskuddBehandling)
                    VedtakResultat.AVSLAG -> avslagSection(tilskudd, tilskuddBehandling)
                }
            }

            innsynsrettSection()
            personopplysningerSection()
            klagerettSection()

            signature(
                Signature(
                    saksbehandler = saksbehandler.personNavn(),
                    beslutter = beslutter.personNavn(),
                    enhet = gjennomforing.ansvarligEnhet.navn,
                ),
            )
        }
    }

    private fun PdfDocumentContentBuilder.innvilgelseSection(
        tilskudd: TilskuddDbo,
        tilskuddBehandling: TilskuddBehandling,
    ) {
        val belop = requireNotNull(tilskudd.utbetalingBelop?.belop) {
            "Innvilget tilskudd beløp var null"
        }
        val valuta = tilskudd.utbetalingBelop.valuta.name

        section(
            "Ditt krav om ${tilskudd.tilskuddOpplaeringType.toDisplayName()} er innvilget for perioden ${tilskuddBehandling.periode.formatPeriode()}.",
        ) {
            paragraph { regular("Beløp til utbetaling: $belop $valuta") }
            paragraph {
                regular(
                    "Vi utbetaler til kontonummeret du har registrert hos Nav. Du kan bare registrere ett " +
                        "kontonummer hos oss. Du kan se, endre og registrere kontonummeret ditt på nav.no. Hvis du " +
                        "ikke har et kontonummer må du ta kontakt med oss.",
                )
            }
            paragraph { regular(HJEMMEL) }
        }
    }

    private fun PdfDocumentContentBuilder.avslagSection(
        tilskudd: TilskuddDbo,
        tilskuddBehandling: TilskuddBehandling,
    ) {
        section(
            "Ditt krav om ${tilskudd.tilskuddOpplaeringType.toDisplayName()} er avslått for perioden ${tilskuddBehandling.periode.formatPeriode()}.",
        ) {
            paragraph { regular("Begrunnelse:") }
            paragraph { regular(tilskudd.kommentarVedtaksbrev.orEmpty()) }
            paragraph { regular(HJEMMEL) }
        }
    }

    private fun PdfDocumentContentBuilder.innsynsrettSection() {
        section("Du har rett til innsyn i saken din") {
            paragraph {
                regular(
                    "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. " +
                        "Kontakt oss om du vil se dokumentene i saken din. Ta kontakt på nav.no/kontakt eller på " +
                        "telefon 55 55 33 33. Du kan lese mer om innsynsretten på nav.no/personvernerklaering.",
                )
            }
        }
    }

    private fun PdfDocumentContentBuilder.personopplysningerSection() {
        section("Du har rettigheter knyttet til personopplysningene dine") {
            paragraph {
                regular(
                    "Du finner informasjon om hvordan Nav behandler personopplysningene dine, og hvilke " +
                        "rettigheter du har, på nav.no/personvernerklaering. Nav kan veilede deg på telefon " +
                        "55 55 33 33 om hvordan Nav behandler personopplysninger.",
                )
            }
        }
    }

    private fun PdfDocumentContentBuilder.klagerettSection() {
        section("Du kan klage på vedtaket") {
            paragraph {
                regular(
                    "Hvis du mener vedtaket er feil, kan du klage innen [antall uker fylles ut av breveieren] " +
                        "uker fra den datoen vedtaket har kommet fram til deg. Dette følger av [sett inn " +
                        "lovhenvisning]. Du finner skjema og informasjon på nav.no/klage.",
                )
            }
            paragraph {
                regular(
                    "Nav kan veilede deg på telefon om hvordan du sender en klage. Nav-kontoret ditt kan også " +
                        "hjelpe deg med å skrive en klage. Kontakt oss på telefon 55 55 33 33.",
                )
            }
            paragraph {
                regular(
                    "Hvis du får medhold i klagen, kan du få dekket vesentlige utgifter som har vært nødvendige " +
                        "for å få endret vedtaket, for eksempel hjelp fra advokat. Du kan ha krav på fri rettshjelp " +
                        "etter rettshjelploven. Du kan få mer informasjon om denne ordningen hos advokater, " +
                        "statsforvalteren eller Nav.",
                )
            }
            paragraph { regular("Du kan lese om saksomkostninger i forvaltningsloven § 36.") }
            paragraph { regular("Hvis du sender klage i posten, må du signere klagen.") }
            paragraph {
                regular("Mer informasjon om klagerettigheter finner du på nav.no/klagerettigheter.")
            }
        }
    }

    private fun AgentDto.personNavn(): String? = when (agent) {
        is NavIdent -> formatNavn(navn)
        else -> null
    }

    private fun formatNavn(fulltNavn: String): String {
        val parts = fulltNavn.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return parts.joinToString(" ")
    }

    private fun Opplaeringtilskudd.Kode.toDisplayName(): String = when (this) {
        Opplaeringtilskudd.Kode.SKOLEPENGER -> "Skolepenger"
        Opplaeringtilskudd.Kode.STUDIEREISE -> "Studiereise"
        Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> "Eksamensgebyr"
        Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> "Semesteravgift"
        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> "Integrert botilbud"
    }
}

private const val HJEMMEL =
    "Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak (tiltaksforskriften) § 7-5, " +
        "jf. lov om arbeidsmarkedstjenester (arbeidsmarkedsloven) § 13."
