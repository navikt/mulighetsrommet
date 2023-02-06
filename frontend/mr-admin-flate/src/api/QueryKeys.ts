import {
  Tiltakstypekategori,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";

export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  tiltakstyper: (
    sokestreng: string,
    status: Tiltakstypestatus,
    tags: string[],
    kategori?: Tiltakstypekategori,
    page?: number
  ) => [sokestreng, status, kategori, tags, page, "tiltakstyper"] as const,
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
  tags: ["tags"] as const,
};
