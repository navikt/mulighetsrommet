import {
  ArrangorDto,
  AvtaleStatusType,
  Avtaletype,
  GjennomforingStatusType,
  NavEnhetDto,
  NavEnhetType,
  TiltakstypeDto,
} from "@tiltaksadministrasjon/api-client";
import { avtaletypeTilTekst } from "./Utils";

export const TILTAKSGJENNOMFORING_STATUS_OPTIONS: {
  label: string;
  value: GjennomforingStatusType;
}[] = [
  {
    label: "Gjennomføres",
    value: GjennomforingStatusType.GJENNOMFORES,
  },
  {
    label: "Avlyst",
    value: GjennomforingStatusType.AVLYST,
  },
  {
    label: "Avsluttet",
    value: GjennomforingStatusType.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: GjennomforingStatusType.AVBRUTT,
  },
];

export const AVTALE_STATUS_OPTIONS: { label: string; value: AvtaleStatusType }[] = [
  {
    label: "Aktiv",
    value: AvtaleStatusType.AKTIV,
  },
  {
    label: "Avsluttet",
    value: AvtaleStatusType.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: AvtaleStatusType.AVBRUTT,
  },
  {
    label: "Utkast",
    value: AvtaleStatusType.UTKAST,
  },
];

export const AVTALE_TYPE_OPTIONS: { label: string; value: Avtaletype }[] = [
  {
    label: avtaletypeTilTekst(Avtaletype.AVTALE),
    value: Avtaletype.AVTALE,
  },
  {
    label: avtaletypeTilTekst(Avtaletype.FORHANDSGODKJENT),
    value: Avtaletype.FORHANDSGODKJENT,
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

export function regionOptions(enheter: NavEnhetDto[]) {
  return enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .sort()
    .map((enhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
}

export function tiltakstypeOptions(tiltakstyper: TiltakstypeDto[]) {
  return tiltakstyper.sort().map((tiltakstype) => ({
    label: tiltakstype.navn,
    value: tiltakstype.id,
  }));
}

export function arrangorOptions(arrangorer: ArrangorDto[]) {
  return arrangorer.sort().map((arrangor) => ({
    label: arrangor.navn,
    value: arrangor.id,
  }));
}
