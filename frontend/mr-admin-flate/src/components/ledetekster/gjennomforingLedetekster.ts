export const gjennomforingTekster = {
  tiltaksnavnLabel: "Tiltaksnavn",
  tiltaksnummerLabel: "Tiltaksnummer",
  avtaleLabel: "Avtale",
  avtaleMedTiltakstype: (tiltakstype: string) => `Avtale (tiltakstype: ${tiltakstype})`,
  ingenAvtaleForGjennomforingenLabel: "Ingen avtale for gjennomføringen",
  tiltakstypeLabel: "Tiltakstype",
  oppstartstypeLabel: "Oppstartstype",
  avtaleStartdatoLabel: "Avtalens startdato",
  avtaleSluttdatoLabel: "Avtalens sluttdato",
  startdatoLabel: "Startdato",
  sluttdatoLabel: "Sluttdato",
  antallPlasserLabel: "Antall plasser",
  deltidsprosentLabel: "Deltidsprosent",
  apentForPameldingLabel: "Åpent for påmelding",
  estimertVentetidLabel: "Estimert ventetid",
  administratorerForGjennomforingenLabel: "Administratorer for gjennomføringen",
  ingenAdministratorerSattForGjennomforingenLabel: "Ingen administratorer satt for gjennomføringen",
  tilgjengeligIModiaLabel: "Tilgjengelig i Modia for:",
  navEnheterKontorerLabel: "Nav-enheter (kontorer)",
  ansvarligEnhetFraArenaLabel: "Ansvarlig enhet fra Arena",
  kontaktpersonNav: {
    navnLabel: "Kontaktperson i Nav",
    omradeLabel: "Område",
    beskrivelseLabel: "Beskrivelse",
  },
  tiltaksarrangorHovedenhetLabel: "Tiltaksarrangør hovedenhet",
  tiltaksarrangorUnderenhetLabel: "Tiltaksarrangør underenhet",
  kontaktpersonerHosTiltaksarrangorLabel: "Kontaktpersoner hos tiltaksarrangør",
  stedForGjennomforingLabel: "Sted for gjennomføring",
  kurstypeLabel: "Kurstype",
  forerkortLabel: "Førerkort",
  sertifiseringerLabel: "Sertifiseringer",
  norskproveLabel: "Norskprøve",
  innholdElementerLabel: "Elementer i kurset",
  amoKategoriseringMangler: "Du må velge kurstype for avtalen",
} as const;
