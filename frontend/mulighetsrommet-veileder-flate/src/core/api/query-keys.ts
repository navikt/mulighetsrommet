import { JoyrideType, NavEnhetStatus, NavEnhetType } from "mulighetsrommet-api-client";
import { ArbeidsmarkedstiltakFilter } from "../../hooks/useArbeidsmarkedstiltakFilter";

export const QueryKeys = {
  SanityQuery: "sanityQuery",
  Brukerdata: "brukerdata",
  Veilederdata: "veilederdata",
  Historikk: "historikk",
  DeltMedBrukerStatus: "deltMedBrukerStatus",
  AlleDeltMedBrukerStatus: "alleDeltMedBrukerStatus",
  sanity: {
    innsatsgrupper: ["innsatsgrupper"],
    tiltakstyper: ["tiltakstyper"],
    tiltaksgjennomforinger: (tiltaksgjennomforingsfilter?: ArbeidsmarkedstiltakFilter) => [
      "tiltaksgjennomforinger",
      { ...tiltaksgjennomforingsfilter },
    ],
    tiltaksgjennomforingerPreview: (
      tiltaksgjennomforingsfilter?: ArbeidsmarkedstiltakFilter,
      geografiskEnhet?: string,
    ) => ["tiltaksgjennomforinger", "preview", { ...tiltaksgjennomforingsfilter, geografiskEnhet }],
    tiltaksgjennomforing: (id: string) => ["tiltaksgjennomforing", id],
    tiltaksgjennomforingPreview: (id: string) => ["tiltaksgjennomforing", "preview", id],
  },
  features: (feature: string) => [feature, "feature"],
  navEnheter: (statuser: NavEnhetStatus[], typer: NavEnhetType[]) => [
    statuser,
    typer,
    "navEnheter",
  ],
  navRegioner: ["navRegioner"],
  oppskrifter: (tiltakstypeId: string) => [tiltakstypeId, "oppskrifter"],
  harFullfortJoyride: (joyrideType: JoyrideType) => [joyrideType, "joyride"],
  overordnetEnhet: (enhetsnummer: string) => ["overordnetEnhet", enhetsnummer],
};
