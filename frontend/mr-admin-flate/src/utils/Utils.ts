import { TiltaksgjennomforingStatus } from "mulighetsrommet-api-client/build/models/TiltaksgjennomforingStatus";

export function capitalize(text?: string): string {
  return text
    ? text.slice(0, 1).toUpperCase() + text.slice(1, text.length).toLowerCase()
    : "";
}

export function capitalizeEveryWord(
  text: string = "",
  ignoreWords: string[] = []
): string {
  return text
    .split(" ")
    .map((it) => {
      if (ignoreWords.includes(it.toLowerCase())) {
        return it.toLowerCase();
      }
      return capitalize(it);
    })
    .join(" ");
}

export function formaterDato(dato?: string | Date, fallback = ""): string {
  if (!dato) return fallback;

  const result = new Date(dato).toLocaleString("no-NO", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  if (result === "Invalid Date") {
    return fallback;
  }

  return result;
}

export function formaterTall(tall: number) {
  return Intl.NumberFormat("no-nb").format(tall);
}

export function kalkulerStatusBasertPaaFraOgTilDato(
  datoer: {
    fraDato: string;
    tilDato: string;
  },
  now: Date = new Date()
): "Aktiv" | "Planlagt" | "Avsluttet" | " - " {
  const { fraDato, tilDato } = datoer;
  const fraDatoAsDate = new Date(fraDato);
  const tilDatoAsDate = new Date(tilDato);

  if (now >= fraDatoAsDate && now <= tilDatoAsDate) {
    return "Aktiv";
  } else if (now < fraDatoAsDate) {
    return "Planlagt";
  } else if (now > tilDatoAsDate) {
    return "Avsluttet";
  } else {
    return " - ";
  }
}

export const resetPaginering = (setPage: (number: number) => void) => {
  setPage(1);
};

export const oversettStatusForTiltaksgjennomforing = (
  status?: TiltaksgjennomforingStatus
) => {
  switch (status) {
    case TiltaksgjennomforingStatus.GJENNOMFORES:
      return "Aktiv";
    case TiltaksgjennomforingStatus.AVBRUTT:
      return "Avbrutt";
    case TiltaksgjennomforingStatus.AVLYST:
      return "Avlyst";
    case TiltaksgjennomforingStatus.AVSLUTTET:
      return "Avsluttet";
    case TiltaksgjennomforingStatus.APENT_FOR_INNSOK:
      return "Åpent for innsøk";
    default:
      return "";
  }
};
