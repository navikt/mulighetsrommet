import { JoyrideType, LagretFilterType } from "@api-client";

export const QueryKeys = {
  Veilederdata: "veilederdata",
  Bruker: (fnr: string) => ["bruker", fnr],
  BrukerHistorikk: (fnr: string, type: "AKTIVE" | "HISTORISKE") => [
    ...QueryKeys.Bruker(fnr),
    type,
    "historikk",
  ],
  Deltakelse: (fnr: string, tiltakId: string) => [...QueryKeys.Bruker(fnr), tiltakId, "deltakelse"],
  DeltMedBrukerStatus: ["deltMedBrukerStatus"],
  AlleDeltMedBrukerStatus: "alleDeltMedBrukerStatus",
  arbeidsmarkedstiltak: {
    innsatsgrupper: ["innsatsgrupper"],
    tiltakstyper: ["tiltakstyper"],
    tiltak: (filter?: object) => ["gjennomforinger", { ...filter }],
    tiltakById: (id: string) => ["gjennomforing", id],
    previewTiltakById: (id: string) => ["gjennomforing", "preview", id],
  },
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
  lagredeFilter: (dokumenttype?: LagretFilterType) =>
    ["lagredeFilter", dokumenttype].filter((part) => part !== undefined),
};
