import {
  AvtaleStatus,
  Avtaletype,
  GjennomforingStatus,
  NavEnhetDto,
  NavEnhetType,
} from "@mr/api-client-v2";
import { ArrangorDto, TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
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

export const AVTALE_STATUS_OPTIONS: { label: string; value: AvtaleStatus }[] = [
  {
    label: "Aktiv",
    value: AvtaleStatus.AKTIV,
  },
  {
    label: "Avsluttet",
    value: AvtaleStatus.AVSLUTTET,
  },
  {
    label: "Avbrutt",
    value: AvtaleStatus.AVBRUTT,
  },
  {
    label: "Utkast",
    value: AvtaleStatus.UTKAST,
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
