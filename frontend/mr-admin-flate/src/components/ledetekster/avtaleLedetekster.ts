import { Avtaletype } from "@mr/api-client-v2";
import { TilsagnType } from "@tiltaksadministrasjon/api-client";

export const avtaletekster = {
  avtalenavnLabel: "Avtalenavn",
  avtalenummerLabel: "Avtalenummer",
  tiltakstypeLabel: "Tiltakstype",
  avtaletypeLabel: "Avtaletype",
  startdatoLabel: "Startdato",
  sluttdatoLabel: (avtaletype: Avtaletype, opsjonerRegistrert: boolean) => {
    if (opsjonerRegistrert) {
      return "Sluttdato*";
    }
    if (avtaletype === Avtaletype.FORHANDSGODKJENT) {
      return "Sluttdato (valgfri)";
    }
    return "Sluttdato";
  },
  maksVarighetLabel: "Maks varighet inkl. opsjon",
  prisOgBetalingLabel: "Pris- og betalingsbetingelser",
  administratorerForAvtalenLabel: "Administratorer for avtalen",
  ingenAdministratorerSattLabel: "Ingen administratorer satt for avtalen",
  sakarkivNummerLabel: "Saksnummer til Avtalesaken i Public 360",
  sakarkivNummerHelpTextTitle: "Hvilket saksnummer er dette?",
  fylkessamarbeidLabel: "Fylkessamarbeid",
  navRegionerLabel: "Nav-regioner",
  navEnheterLabel: "Nav-enheter (kontorer)",
  navAndreEnheterLabel: "Nav-enheter (andre)",
  ansvarligEnhetFraArenaLabel: "Ansvarlig enhet fra Arena",
  tiltaksarrangorHovedenhetLabel: "Tiltaksarrangør hovedenhet",
  tiltaksarrangorUnderenheterLabel: "Tiltaksarrangør underenheter",
  kontaktpersonerHosTiltaksarrangorLabel: "Kontaktpersoner hos tiltaksarrangør",
  avtaltForlengelseLabel: "Avtalt mulighet for forlengelse",
  avtaltPrisLabel: "Avtalt pris",
  arrangorManglerVarsel: "Arrangør mangler",
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
    heading: "Betalingsbetingelser",
    valuta: {
      label: "Valuta",
    },
    pris: {
      label: "Pris",
    },
    sats: {
      label: "Sats",
    },
    periodeStart: {
      label: "Gjelder fra",
    },
    periodeSlutt: {
      label: "Gjelder til",
    },
  },
  tilsagn: {
    type: (type: TilsagnType): string => {
      switch (type) {
        case TilsagnType.TILSAGN:
          return "Tilsagn";
        case TilsagnType.EKSTRATILSAGN:
          return "Ekstratilsagn";
        case TilsagnType.INVESTERING:
          return "Tilsagn for investeringer";
      }
    },
  },
} as const;
