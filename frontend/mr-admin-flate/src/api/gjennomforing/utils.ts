import {
  GjennomforingDto,
  GjennomforingEnkeltplassDto,
  GjennomforingGruppeDto,
} from "@tiltaksadministrasjon/api-client";

export function isGruppetiltak(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingGruppeDto {
  return gjennomforing.type === "GjennomforingGruppeDto";
}

export function isEnkeltplass(
  gjennomforing: GjennomforingDto,
): gjennomforing is GjennomforingEnkeltplassDto {
  return gjennomforing.type === "GjennomforingEnkeltplassDto";
}
