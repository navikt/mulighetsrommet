package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.pdfgen.Deltaker
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.Signature
import no.nav.mulighetsrommet.api.pdfgen.TopSection
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.BrukerUtbetalingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.TilskuddBrevVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.task.VedtaksbrevInnhold
import no.nav.mulighetsrommet.model.Periode

object TilskuddVedtakToPdfDocumentContentMapper {
    fun toPdfDocumentContent(
        innhold: VedtaksbrevInnhold,
    ): PdfDocumentContent {
        return PdfDocumentContent.create(
            title = "Vedtak om tilskudd til opplæring – ${innhold.deltakerPersonalia.navn} (${innhold.tiltak.lopenummer})",
            subject = "Vedtak om tilskudd til opplæring",
            description = "Vedtak om tilskudd til opplæring til ${innhold.deltakerPersonalia.navn}",
            author = "Nav",
        ) {
            topSection(
                TopSection(
                    reference = innhold.tiltak.lopenummer,
                    date = innhold.besluttetTidspunkt.toLocalDate().toString(),
                    deltaker = Deltaker(
                        navn = innhold.deltakerPersonalia.navn,
                        norskIdent = innhold.deltakerPersonalia.norskIdent,
                    ),
                ),
            )

            mainSection("Vedtak om tilskudd til opplæring")

            innhold.tilskuddvedtak.forEach { tilskudd ->
                when (tilskudd.vedtakResultat) {
                    VedtakResultat.INNVILGELSE -> innvilgelseSection(
                        tilskudd,
                        innhold.tiltak.periode,
                        innhold.arrangorNavn,
                        innhold.tiltak.navn,
                    )

                    VedtakResultat.AVSLAG -> avslagSection(tilskudd)
                }
            }

            klagerettSection()
            innsynsrettSection()
            personopplysningerSection()
            sporsmalSection()

            signature(
                Signature(
                    saksbehandler = innhold.saksbehandler,
                    beslutter = innhold.beslutter,
                    enhet = innhold.behandlendeEnhet,
                ),
            )
        }
    }

    private fun PdfDocumentContentBuilder.innvilgelseSection(
        tilskudd: TilskuddBrevVedtak,
        tiltaksperiode: Periode,
        arrangorNavn: String,
        tiltaksnavn: String,
    ) {
        val belop = requireNotNull(tilskudd.belop) {
            "Innvilget tilskuddsbeløp var null"
        }

        section(
            "Nav har innvilget ${tilskudd.tilskuddType} for perioden ${tilskudd.periode.formatPeriode()}.",
        ) {
            paragraph { regular("Beløp til utbetaling: ${belop.belop} ${belop.valuta}. Beløpet er beregnet ut fra mottatt faktura.") }
            if (tilskudd.begrunnelse != null) {
                paragraph { regular(tilskudd.begrunnelse) }
            }
        }
        section(
            "Slik har vi vurdert saken din",
            level = 3,
        ) {
            paragraph {
                regular(
                    "Du får dekket ${tilskudd.tilskuddType} for å gjennomføre tiltaket $tiltaksnavn ved $arrangorNavn i perioden ${
                        tiltaksperiode.formatPeriode()
                    }.",
                )
            }
            paragraph {
                regular(
                    "Vedtaket gjelder for perioden vi har mottatt faktura for. Hvis Nav skal gi " +
                        "tilskudd senere i utdanningsløpet, må du sende inn ny faktura når du mottar denne.",
                )
            }
            paragraph { regular(HJEMMEL) }
        }
        when (tilskudd.utbetalingMottaker) {
            TilskuddMottaker.BRUKER -> utbetalingBrukerSection(tilskudd, arrangorNavn)

            TilskuddMottaker.ARRANGOR -> utbetalingArrangorSection(
                arrangorNavn,
            )
        }
    }

    private fun PdfDocumentContentBuilder.avslagSection(
        tilskudd: TilskuddBrevVedtak,
    ) {
        check(tilskudd.begrunnelse != null) {
            "Avslag må ha begrunnelse"
        }
        section(
            "Ditt krav om ${tilskudd.tilskuddType} er avslått for perioden ${tilskudd.periode.formatPeriode()}.",
        ) {
            paragraph { regular("Nav har avslått tilskuddet fordi det ikke er dokumentert at vilkårene er oppfylt. ") }
            paragraph { regular(tilskudd.begrunnelse) }
            paragraph { regular(HJEMMEL) }
        }
    }

    private fun PdfDocumentContentBuilder.utbetalingBrukerSection(tilskudd: TilskuddBrevVedtak, arrangorNavn: String) {
        section("Når får du pengene?", level = 3) {
            paragraph {
                regular(
                    "Pengene vil vanligvis utbetales til kontoen din etter to til tre virkedager.  ",
                )
            }
            paragraph {
                regular(
                    "Du kan se alle utbetalingene dine på nav.no/minside. Der kan du også endre kontonummer. " +
                        "Hvis du har reservert deg mot digital kommunikasjon fra det offentlige, får du " +
                        "utbetalingsmelding i posten. Du kan også melde fra om endring i kontonummer via post. " +
                        "Kontakt oss hvis du trenger hjelp til dette.",
                )
            }
        }
        dinePlikterSection(tilskudd, arrangorNavn)
    }
}

private fun PdfDocumentContentBuilder.utbetalingArrangorSection(arrangorNavn: String) {
    section("Pengene utbetales til utdanningsstedet ditt", level = 3) {
        paragraph {
            regular(
                "Nav utbetaler tilskuddet til $arrangorNavn.",
            )
        }
    }
}

private fun PdfDocumentContentBuilder.dinePlikterSection(tilskudd: TilskuddBrevVedtak, arrangorNavn: String) {
    section("Dine plikter", level = 3) {
        if (tilskudd.brukerUtbetalingType == BrukerUtbetalingType.FORSKUDD) {
            paragraph {
                regular(
                    "Pengene skal kun brukes til å betale ${tilskudd.tilskuddType} til $arrangorNavn. " +
                        "Hvis du bruker pengene til noe annet, vil Nav kunne avbryte tiltaket.",
                )
            }
        }
        paragraph {
            regular(
                "Hvis du har fått utbetalt for mye, må du vanligvis betale tilbake pengene. Det er derfor viktig " +
                    "at du selv følger med på utbetalinger fra Nav og melder fra om eventuelle feil.",
            )
        }
    }
}

private fun PdfDocumentContentBuilder.innsynsrettSection() {
    section("Du har rett til innsyn i saken din") {
        paragraph {
            regular(
                "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18. " +
                    "Du har tilgang til mange av dokumentene dine digitalt på nav.no. Hvis det er dokumenter du ikke finner, " +
                    "kan du be oss om innsyn i disse. Du kan lese mer om retten til innsyn på nav.no/innsynskrav.",
            )
        }
    }
}

private fun PdfDocumentContentBuilder.personopplysningerSection() {
    section("Du har rettigheter knyttet til personopplysningene dine") {
        paragraph {
            regular(
                "Du finner informasjon om hvordan Nav behandler personopplysningene dine, og hvilke " +
                    "rettigheter du har, på nav.no/personvernerklaering.",
            )
        }
    }
}

private fun PdfDocumentContentBuilder.klagerettSection() {
    section("Du kan klage på vedtaket") {
        paragraph {
            regular(
                "Hvis du mener vedtaket er feil, har du rett til å klage. Du må sende klagen innen 6 uker fra den datoen vedtaket har " +
                    "kommet fram til deg. Dette følger av arbeidsmarkedsloven § 17. Du finner mer informasjon om hvordan du kan klage på nav.no/klage.",
            )
        }
        paragraph {
            regular(
                "Nav kan veilede deg på telefon hvis du trenger hjelp til å sende en klage. Du kan også få hjelp av " +
                    "Nav-kontoret ditt til å skrive en klage. Hvis du velger å sende klagen på papir, må du huske å signere den.",
            )
        }
        paragraph {
            regular(
                "Når du klager, vil den Nav-enheten som har skrevet vedtaket, vurdere saken din på nytt. Hvis du får " +
                    "medhold i klagen din, mottar du et nytt vedtak. Hvis du ikke får medhold, sender vi klagen videre til " +
                    "Nav klageinstans. Du får brev når saken er sendt dit. I brevet står det hva som skjer videre. Før en " +
                    "sak kan bringes inn for domstolene, må du ha brukt muligheten til å klage og anke. Dette følger av " +
                    "arbeidsmarkedsloven § 17"
            )
        }
        paragraph {
            regular(
                "Hvis du får medhold i klagen, kan du ha rett til å få dekket utgifter som har vært nødvendige for å " +
                    "få endret vedtaket, for eksempel hjelp fra advokat. Du kan lese mer om sakskostnader i " +
                    "forvaltningsloven § 36. Du kan ha krav på fri rettshjelp etter rettshjelploven. Du kan få mer " +
                    "informasjon om denne ordningen hos advokater, statsforvalteren eller Nav. "
            )
        }

        paragraph { regular("Les mer på nav.no/klagerettigheter.") }
    }
}

private fun PdfDocumentContentBuilder.sporsmalSection() {
    section("Har du spørsmål?") {
        paragraph {
            regular(
                "Du finner mer informasjon på nav.no/opplaring.",
            )
        }
        paragraph { regular("På nav.no/kontakt kan du chatte eller skrive til oss.") }
        paragraph { regular("Hvis du ikke finner svar på nav.no, kan du ringe oss på telefon 55 55 33 33 hverdager 09.00-15.00.") }
    }
}

private const val HJEMMEL =
    "Vedtaket er fattet med hjemmel i forskrift om arbeidsmarkedstiltak (tiltaksforskriften) § 7-5, " +
        "jf. lov om arbeidsmarkedstjenester (arbeidsmarkedsloven) § 13."
