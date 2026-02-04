import {
  GjennomforingDto,
  GjennomforingEnkeltplassDto,
  GjennomforingAvtaleDto,
} from "@tiltaksadministrasjon/api-client";

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
