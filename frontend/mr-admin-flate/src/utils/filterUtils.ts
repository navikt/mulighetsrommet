import {
  Arrangor,
  Avtalestatus,
  Avtaletype,
  NavEnhet,
  NavEnhetType,
  TiltaksgjennomforingStatus,
  TiltakstypeDto,
} from "@mr/api-client";
import { avtaletypeTilTekst } from "./Utils";

export const TILTAKSGJENNOMFORING_STATUS_OPTIONS: {
  label: string;
  value: TiltaksgjennomforingStatus;
}[] = [
  {
    label: "Planlagt",
    value: TiltaksgjennomforingStatus.PLANLAGT,
  },
  {
    label: "Gjennomføres",
    value: TiltaksgjennomforingStatus.GJENNOMFORES,
  },
  {
    label: "Avlyst",
    value: TiltaksgjennomforingStatus.AVLYST,
  },
  {
    label: "Avsluttet",
    value: TiltaksgjennomforingStatus.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: TiltaksgjennomforingStatus.AVBRUTT,
  },
];

export const AVTALE_STATUS_OPTIONS: { label: string; value: Avtalestatus }[] = [
  {
    label: "Aktiv",
    value: Avtalestatus.AKTIV,
  },
  {
    label: "Avsluttet",
    value: Avtalestatus.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: Avtalestatus.AVBRUTT,
  },
];

export const AVTALE_TYPE_OPTIONS: { label: string; value: Avtaletype }[] = [
  {
    label: avtaletypeTilTekst(Avtaletype.AVTALE),
    value: Avtaletype.AVTALE,
  },
  {
    label: avtaletypeTilTekst(Avtaletype.FORHAANDSGODKJENT),
    value: Avtaletype.FORHAANDSGODKJENT,
  },
  {
    label: avtaletypeTilTekst(Avtaletype.OFFENTLIG_OFFENTLIG),
    value: Avtaletype.OFFENTLIG_OFFENTLIG,
  },
  {
    label: avtaletypeTilTekst(Avtaletype.RAMMEAVTALE),
    value: Avtaletype.RAMMEAVTALE,
  },
];

export function regionOptions(enheter: NavEnhet[]) {
  return enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .sort()
    .map((enhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
}

export function tiltakstypeOptions(tiltakstyper: TiltakstypeDto[]) {
  return (
    tiltakstyper.sort().map((tiltakstype) => ({
      label: tiltakstype.navn,
      value: tiltakstype.id,
    })) || []
  );
}

export function arrangorOptions(arrangorer: Arrangor[]) {
  return arrangorer.sort().map((arrangor) => ({
    label: arrangor.navn,
    value: arrangor.id,
  }));
}

export function getPublisertStatus(statuser: string[] = []): boolean | null {
  if (statuser.length === 0) return null;

  if (statuser.every((status) => status === "publisert")) return true;

  if (statuser.every((status) => status === "ikke-publisert")) return false;

  return null;
}
