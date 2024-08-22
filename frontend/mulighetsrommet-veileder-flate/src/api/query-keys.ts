import { JoyrideType, NavEnhetStatus, NavEnhetType } from "@mr/api-client";

export const QueryKeys = {
  Veilederdata: "veilederdata",
  Bruker: (fnr: string) => ["bruker", fnr],
  BrukerHistorikk: (fnr: string) => [...QueryKeys.Bruker(fnr), "historikk"],
  BrukerDeltakelser: (fnr: string) => [...QueryKeys.Bruker(fnr), "deltakelser"],
  DeltMedBrukerStatus: ["deltMedBrukerStatus"],
  AlleDeltMedBrukerStatus: "alleDeltMedBrukerStatus",
  arbeidsmarkedstiltak: {
    innsatsgrupper: ["innsatsgrupper"],
    tiltakstyper: ["tiltakstyper"],
    tiltaksgjennomforinger: (tiltaksgjennomforingsfilter?: object) => [
      "tiltaksgjennomforinger",
      { ...tiltaksgjennomforingsfilter },
    ],
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
  behandlingAvPersonopplysninger: (avtaleId?: string) => [
    "behandlingAvPersonopplysninger",
    avtaleId,
  ],
  deltMedBrukerHistorikk: (norskIdent: string) => ["deltMedBrukerHistorikk", norskIdent],
  tiltakstyperSomStotterPameldingIModia: () => ["tiltakstyperSomStotterPameldingIModia"],
  tiltakstyperSomSnartStotterPameldingIModia: () => ["tiltakstyperSomSnartStotterPameldingIModia"],
};
