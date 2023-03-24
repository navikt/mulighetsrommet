import {
  Avtalestatus,
  SorteringTiltakstyper,
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { Tiltaksgjennomforingfilter } from "./atoms";

export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  nokkeltallTiltakstype: (id?: string) =>
    [id, "nokkeltallTiltakstype"] as const,
  alleTiltakstyper: () => ["tiltakstyper"],
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
  tiltaksgjennomforingerByTiltakstypeId: (id: string, page?: number) =>
    [id, page, "tiltaksgjennomforinger"] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
  tiltaksgjennomforingerByAnsatt: (page?: number) =>
    [
      "tiltaksgjennomforingerForAnsatt",
      page,
      "tiltaksgjennomforinger",
    ] as const,
  avtaler: (
    tiltakstypeId: string,
    sok: string,
    status: Avtalestatus,
    enhet: string,
    sortering: string,
    page: number
  ) => [sok, status, enhet, sortering, tiltakstypeId, page, "avtaler"],
  avtale: (avtaleId: string) => [avtaleId, "avtale"],
  nokkeltallAvtale: (avtaleId: string) => [avtaleId, "nokkeltallAvtale"],
  enheter: () => ["enheter"],
};
