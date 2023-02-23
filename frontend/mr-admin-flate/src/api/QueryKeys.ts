import {
  Avtalestatus,
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";

export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  tiltakstyper: (
    sokestreng: string,
    status: Tiltakstypestatus,
    kategori?: Tiltakstypekategori,
    page?: number
  ) => [sokestreng, status, kategori, page, "tiltakstyper"] as const,
  tiltaksgjennomforinger: (page?: number) =>
    [page, "tiltaksgjennomforinger"] as const,
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
  enheter: () => ["enheter"],
};
