import {
  GjennomforingAvtaleDetaljerDto,
  GjennomforingAvtaleDto,
  GjennomforingDetaljerDto,
  GjennomforingEnkeltplassDetaljerDto,
  GjennomforingEnkeltplassDto,
  GjennomforingKompaktDto,
  GjennomforingType,
} from "@tiltaksadministrasjon/api-client";

export type GjennomforingDto = GjennomforingEnkeltplassDto | GjennomforingAvtaleDto;

export function isEnkeltplassKompakt(gjennomforing: GjennomforingKompaktDto): boolean {
  return gjennomforing.type === GjennomforingType.ENKELTPLASS;
}

export function isGjennomforingAvtaleDetaljer(
  detaljer: GjennomforingDetaljerDto,
): detaljer is GjennomforingAvtaleDetaljerDto & {
  type: "GjennomforingAvtaleDetaljerDto";
} {
  return detaljer.type === "GjennomforingAvtaleDetaljerDto";
}

export function isGjennomforingEnkeltplassDetaljer(
  detaljer: GjennomforingDetaljerDto,
): detaljer is GjennomforingEnkeltplassDetaljerDto & {
  type: "GjennomforingEnkeltplassDetaljerDto";
} {
  return detaljer.type === "GjennomforingEnkeltplassDetaljerDto";
}
