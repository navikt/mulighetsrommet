import { Avtaletype } from "@mr/api-client";

export const avtaletekster = {
  avtalenavnLabel: "Avtalenavn",
  avtalenummerLabel: "Avtalenummer",
  tiltakstypeLabel: "Tiltakstype",
  avtaletypeLabel: "Avtaletype",
  startdatoLabel: "Startdato",
  sluttdatoLabel: (opsjonerRegistrert: boolean) =>
    opsjonerRegistrert ? "Sluttdato*" : "Sluttdato",
  valgfriSluttdatoLabel: (avtaletype: Avtaletype) =>
    avtaletype === Avtaletype.FORHAANDSGODKJENT ? "Sluttdato (valgfri)" : "Sluttdato",
  maksVarighetLabel: "Maks varighet inkl. opsjon",
  prisOgBetalingLabel: "Pris- og betalingsbetingelser",
  administratorerForAvtalenLabel: "Administratorer for avtalen",
  ingenAdministratorerSattLabel: "Ingen administratorer satt for avtalen",
  websaknummerLabel: "Saksnummer til Avtalesaken i Websak",
  websaknummerHelpTextTitle: "Hvilket saksnummer er dette?",
  fylkessamarbeidLabel: "Fylkessamarbeid",
  navRegionerLabel: "NAV-regioner",
  navEnheterLabel: "NAV-enheter (kontorer)",
  ansvarligEnhetFraArenaLabel: "Ansvarlig enhet fra Arena",
  tiltaksarrangorHovedenhetLabel: "Tiltaksarrangør hovedenhet",
  tiltaksarrangorUnderenheterLabel: "Tiltaksarrangør underenheter",
  kontaktpersonerHosTiltaksarrangorLabel: "Kontaktpersoner hos tiltaksarrangør",
  avtaltForlengelseLabel: "Avtalt mulighet for forlengelse",
  utdanning: {
    utdanningsprogram: {
      label: "Utdanningsprogram",
      velg: "Velg utdanningsprogram",
    },
    laerefag: {
      label: "Lærefag",
      velg: "Velg lærefag",
    },
  },
} as const;
