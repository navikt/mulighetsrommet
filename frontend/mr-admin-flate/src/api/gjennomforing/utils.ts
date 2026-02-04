import {
  GjennomforingDto,
  EnkeltplassGjennomforingDto,
  AvtaleGjennomforingDto,
} from "@tiltaksadministrasjon/api-client";

export function isGruppetiltak(
  gjennomforing: GjennomforingDto,
): gjennomforing is AvtaleGjennomforingDto {
  return gjennomforing.type === "AvtaleGjennomforingDto";
}

export function isEnkeltplass(
  gjennomforing: GjennomforingDto,
): gjennomforing is EnkeltplassGjennomforingDto {
  return gjennomforing.type === "EnkeltplassGjennomforingDto";
}
