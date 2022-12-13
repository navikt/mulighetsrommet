export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  tiltakstyper: (page?: number) => [page, "tiltakstyper"] as const,
  tiltaksgjennomforinger: (page?: number) =>
    [page, "tiltaksgjennomforinger"] as const,
  tiltaksgjennomforing: (id?: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: ["ansatt"] as const,
  tiltaksgjennomforingerByTiltakskode: (tiltakskode: string, page?: number) =>
    [tiltakskode, page, "tiltaksgjennomforinger"] as const,
};
