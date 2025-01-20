import { JoyrideType, LagretDokumenttype, NavEnhetStatus, NavEnhetType } from "@mr/api-client-v2";

export const QueryKeys = {
  Veilederdata: "veilederdata",
  Bruker: (fnr: string) => ["bruker", fnr],
  BrukerHistorikk: (fnr: string, type: "AKTIVE" | "HISTORISKE") => [
    ...QueryKeys.Bruker(fnr),
    type,
    "historikk",
  ],
  BrukerDeltakelser: (fnr: string) => [...QueryKeys.Bruker(fnr), "deltakelser"],
  DeltakelseForGjennomforing: (fnr: string, gjennomforingId: string) => [
    ...QueryKeys.Bruker(fnr),
    gjennomforingId,
    "deltakelser-for-gjennomforing",
  ],
  DeltMedBrukerStatus: ["deltMedBrukerStatus"],
  AlleDeltMedBrukerStatus: "alleDeltMedBrukerStatus",
  arbeidsmarkedstiltak: {
    innsatsgrupper: ["innsatsgrupper"],
    tiltakstyper: ["tiltakstyper"],
    tiltak: (filter?: object) => ["gjennomforinger", { ...filter }],
    tiltakById: (id: string) => ["gjennomforing", id],
    previewTiltakById: (id: string) => ["gjennomforing", "preview", id],
  },
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
  lagredeFilter: (dokumenttype: LagretDokumenttype) => ["lagredeFilter", dokumenttype],
};
