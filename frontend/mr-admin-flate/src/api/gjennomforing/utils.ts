import {
  GjennomforingDto,
  GjennomforingEnkeltplassDto,
  GjennomforingAvtaleDto,
  GjennomforingKompaktDto,
  GjennomforingType,
} from "@tiltaksadministrasjon/api-client";

export function isEnkeltplassKompakt(gjennomforing: GjennomforingKompaktDto): boolean {
  return gjennomforing.type === GjennomforingType.ENKELTPLASS;
}

export function isGruppetiltak(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingAvtaleDto {
  return gjennomforing.type === "GjennomforingAvtaleDto";
}

export function isEnkeltplass(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingEnkeltplassDto {
  return gjennomforing.type === "GjennomforingEnkeltplassDto";
}
