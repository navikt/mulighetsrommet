import {
  Arrangor,
  Avtalestatus,
  Avtaletype,
  NavEnhet,
  NavEnhetType,
  GjennomforingStatus,
  TiltakstypeDto,
} from "@mr/api-client-v2";
import { avtaletypeTilTekst } from "./Utils";

export const TILTAKSGJENNOMFORING_STATUS_OPTIONS: {
  label: string;
  value: GjennomforingStatus;
}[] = [
  {
    label: "Gjennomføres",
    value: GjennomforingStatus.GJENNOMFORES,
  },
  {
    label: "Avlyst",
    value: GjennomforingStatus.AVLYST,
  },
  {
    label: "Avsluttet",
    value: GjennomforingStatus.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: GjennomforingStatus.AVBRUTT,
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
