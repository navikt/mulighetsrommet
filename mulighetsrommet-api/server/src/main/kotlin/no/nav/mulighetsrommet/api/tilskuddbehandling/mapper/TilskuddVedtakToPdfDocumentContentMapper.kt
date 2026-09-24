package no.nav.mulighetsrommet.api.tilskuddbehandling.mapper

import no.nav.mulighetsrommet.api.pdfgen.Deltaker
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContent
import no.nav.mulighetsrommet.api.pdfgen.PdfDocumentContentBuilder
import no.nav.mulighetsrommet.api.pdfgen.Signature
import no.nav.mulighetsrommet.api.pdfgen.TopSection
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

            innsynsrettSection()
            personopplysningerSection()
            klagerettSection()
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
            "Innvilget tilskudd beløp var null"
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
            TilskuddMottaker.BRUKER -> utbetalingBrukerSection()

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
            paragraph { regular("Søknaden er avslått fordi det ikke er dokumentert at vilkårene for tilskuddet er oppfylt. ") }
            paragraph { regular(tilskudd.begrunnelse) }
            paragraph { regular(HJEMMEL) }
        }
    }

    private fun PdfDocumentContentBuilder.utbetalingBrukerSection() {
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
                        "utbetalingsmelding i posten. Du kan også melde fra om endring i kontonummer via post.",
                )
            }
            paragraph {
                regular("Kontakt oss på telefon 55 55 33 33 hvis du trenger hjelp. ")
            }
        }
        dinePlikterSection()
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

private fun PdfDocumentContentBuilder.dinePlikterSection() {
    section("Dine plikter", level = 3) {
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
                "Hvis du mener vedtaket er feil, kan du klage innen 6 uker fra den datoen vedtaket har " +
                    "kommet fram til deg. Dette følger av arbeidsmarkedsloven § 17. Du finner skjema og informasjon " +
                    "på nav.no/klage.",
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
