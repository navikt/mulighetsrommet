import { JoyrideType, LagretDokumenttype, NavEnhetStatus, NavEnhetType } from "@mr/api-client";

export const QueryKeys = {
  Veilederdata: "veilederdata",
  Bruker: (fnr: string) => ["bruker", fnr],
  BrukerHistorikk: (fnr: string, type: "AKTIVE" | "HISTORISKE") => [
    ...QueryKeys.Bruker(fnr),
    type,
    "historikk",
  ],
  BrukerDeltakelser: (fnr: string) => [...QueryKeys.Bruker(fnr), "deltakelser"],
  DeltakelseForGjennomforing: (fnr: string, tiltaksgjennomforingId: string) => [
    ...QueryKeys.Bruker(fnr),
    tiltaksgjennomforingId,
    "deltakelser-for-gjennomforing",
  ],
  DeltMedBrukerStatus: ["deltMedBrukerStatus"],
  AlleDeltMedBrukerStatus: "alleDeltMedBrukerStatus",
  arbeidsmarkedstiltak: {
    innsatsgrupper: ["innsatsgrupper"],
    tiltakstyper: ["tiltakstyper"],
    tiltak: (filter?: object) => ["tiltaksgjennomforinger", { ...filter }],
    tiltakById: (id: string) => ["tiltaksgjennomforing", id],
    previewTiltakById: (id: string) => ["tiltaksgjennomforing", "preview", id],
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
