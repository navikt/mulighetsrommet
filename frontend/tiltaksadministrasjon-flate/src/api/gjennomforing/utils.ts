import {
  GjennomforingDto,
  GjennomforingKompaktDto,
  GjennomforingType,
} from "@tiltaksadministrasjon/api-client";

type GjennomforingAvtale = Extract<GjennomforingDto, { type: "GjennomforingAvtaleDto" }>;
type GjennomforingEnkeltplass = Extract<GjennomforingDto, { type: "GjennomforingEnkeltplassDto" }>;

export function isEnkeltplassKompakt(gjennomforing: GjennomforingKompaktDto): boolean {
  return gjennomforing.type === GjennomforingType.ENKELTPLASS;
}

export function isGruppetiltak(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingAvtale {
  return gjennomforing.type === "GjennomforingAvtaleDto";
}

export function isEnkeltplass(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingEnkeltplass {
  return gjennomforing.type === "GjennomforingEnkeltplassDto";
}
