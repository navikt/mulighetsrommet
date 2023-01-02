export const QueryKeys = {
  tiltakstype: (id?: string) => [id, "tiltakstype"] as const,
  tiltakstyper: (page?: number) => [page, "tiltakstyper"] as const,
  tiltaksgjennomforinger: (page?: number) =>
    [page, "tiltaksgjennomforinger"] as const,
  tiltaksgjennomforing: (id?: string) => [id, "tiltaksgjennomforing"] as const,
  ansatt: ["ansatt"] as const,
  tiltaksgjennomforingerByTiltakstypeId: (id: string, page?: number) =>
    [id, page, "tiltaksgjennomforinger"] as const,
  tiltaksgjennomforingerByEnhet: (enhet: string = "enhet", page?: number) =>
    [enhet, page, "tiltaksgjennomforinger"] as const,
};
