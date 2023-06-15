import {
  SorteringTiltakstyper,
  Tiltakstypekategori,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { AvtaleFilterProps, Tiltaksgjennomforingfilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  nokkeltallTiltakstype: (id?: string) =>
    [id, "nokkeltallTiltakstype"] as const,
  tiltakstyper: (
    sokestreng: string,
    status?: Tiltakstypestatus,
    kategori?: Tiltakstypekategori,
    sortering?: SorteringTiltakstyper,
    page?: number
  ) => [sokestreng, status, kategori, sortering, page, "tiltakstyper"] as const,
  tiltaksgjennomforinger: (
    tiltaksgjennomforingfilter: Tiltaksgjennomforingfilter,
    page?: number
  ) =>
    [
      { ...tiltaksgjennomforingfilter },
      page,
      "tiltaksgjennomforinger",
    ] as const,
  nokkeltallTiltaksgjennomforing: (id?: string) =>
    [id, "nokkeltallTiltaksgjennomforing"] as const,
  tiltaksgjennomforing: (id?: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: ["ansatt"] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  avtaler: (avtaleFilter: AvtaleFilterProps, page: number) => [
    { ...avtaleFilter },
    page,
    "avtaler",
  ],
  avtale: (avtaleId: string) => [avtaleId, "avtale"],
  nokkeltallAvtale: (avtaleId: string) => [avtaleId, "nokkeltallAvtale"],
  enheter: () => ["enheter"],
  virksomheter: (til: VirksomhetTil) => [til, "virksomheter"],
  antallUlesteNotifikasjoner: () => ["antallUlesteNotifikasjoner"],
  virksomhetSok: (sokestreng: string) => ["virksomhetSok", sokestreng],
  virksomhetOppslag: (orgnr: string) => ["virksometOppslag", orgnr],
  tiltaksgjennomforingerTilAvtale: (filter: string) => [
    "tiltaksgjennomforinger",
    filter,
  ],
  kontaktpersoner: () => ["nav-kontaktpersoner"],
};
