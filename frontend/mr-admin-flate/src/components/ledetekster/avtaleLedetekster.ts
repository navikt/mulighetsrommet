import { Avtaletype, Prismodell } from "@mr/api-client";

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
  navRegionerLabel: "Nav-regioner",
  navEnheterLabel: "Nav-enheter (kontorer)",
  ansvarligEnhetFraArenaLabel: "Ansvarlig enhet fra Arena",
  tiltaksarrangorHovedenhetLabel: "Tiltaksarrangør hovedenhet",
  tiltaksarrangorUnderenheterLabel: "Tiltaksarrangør underenheter",
  kontaktpersonerHosTiltaksarrangorLabel: "Kontaktpersoner hos tiltaksarrangør",
  avtaltForlengelseLabel: "Avtalt mulighet for forlengelse",
  utdanning: {
    utdanningsprogramManglerForAvtale: "Du må velge utdanningsprogram på avtalen",
    utdanningsprogram: {
      label: "Utdanningsprogram",
      velg: "Velg utdanningsprogram",
    },
    laerefag: {
      label: "Lærefag",
      velg: "Velg lærefag",
    },
  },
  prismodell: {
    label: "Prismodell",
    beskrivelse: (prismodell: Prismodell): string => {
      switch (prismodell) {
        case Prismodell.FORHANDSGODKJENT:
          return "Fast sats per tiltaksplass per måned";
        case Prismodell.FRI:
          return "Fri prismodell";
      }
    },
    valuta: {
      label: "Valuta",
    },
    pris: {
      label: "Pris",
    },
    periodeStart: {
      label: "Gjelder fra",
    },
    periodeSlutt: {
      label: "Gjelder til",
    },
  },
} as const;
