import {
  ArrangorDto,
  AvtaleStatusType,
  Avtaletype,
  DeltakerStatusType,
  GjennomforingStatusType,
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

export const ENKELTPLASS_STATUS_OPTIONS: { label: string; value: DeltakerStatusType }[] = [
  { label: "Utkast til påmelding", value: DeltakerStatusType.UTKAST_TIL_PAMELDING },
  { label: "Søkt inn", value: DeltakerStatusType.SOKT_INN },
  { label: "Venter på oppstart", value: DeltakerStatusType.VENTER_PA_OPPSTART },
  { label: "Deltar", value: DeltakerStatusType.DELTAR },
  { label: "Ikke aktuell", value: DeltakerStatusType.IKKE_AKTUELL },
  { label: "Fullført", value: DeltakerStatusType.FULLFORT },
  { label: "Avbrutt", value: DeltakerStatusType.AVBRUTT },
];

// TILTAKSGJENNOMFORING_STATUS_OPTIONS og ENKELTPLASS_STATUS_OPTIONS har begge en AVBRUTT-verdi,
// se de må prefikses for å kunne spores tilbake til riktig filter i API'et
const GJENNOMFORING_STATUS_PREFIX = "gjennomforing:";
const ENKELTPLASS_STATUS_PREFIX = "enkeltplass:";

export function toGjennomforingStatusFilterId(status: GjennomforingStatusType): string {
  return `${GJENNOMFORING_STATUS_PREFIX}${status}`;
}

export function toEnkeltplassStatusFilterId(status: DeltakerStatusType): string {
  return `${ENKELTPLASS_STATUS_PREFIX}${status}`;
}

export function splitGjennomforingStatuser(gjennomforingStatuser: string[]): {
  statuser: GjennomforingStatusType[];
  enkeltplassStatuser: DeltakerStatusType[];
} {
  const statuser = gjennomforingStatuser
    .filter((id) => id.startsWith(GJENNOMFORING_STATUS_PREFIX))
    .map((id) => id.slice(GJENNOMFORING_STATUS_PREFIX.length) as GjennomforingStatusType);
  const enkeltplassStatuser = gjennomforingStatuser
    .filter((id) => id.startsWith(ENKELTPLASS_STATUS_PREFIX))
    .map((id) => id.slice(ENKELTPLASS_STATUS_PREFIX.length) as DeltakerStatusType);
  return { statuser, enkeltplassStatuser };
}

export function gjennomforingStatusLabel(id: string): string {
  if (id.startsWith(GJENNOMFORING_STATUS_PREFIX)) {
    const status = id.slice(GJENNOMFORING_STATUS_PREFIX.length);
    return TILTAKSGJENNOMFORING_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? id;
  }
  if (id.startsWith(ENKELTPLASS_STATUS_PREFIX)) {
    const status = id.slice(ENKELTPLASS_STATUS_PREFIX.length);
    return ENKELTPLASS_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? id;
  }
  return id;
}

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

export function arrangorOptions(arrangorer: ArrangorDto[]) {
  return arrangorer.sort().map((arrangor) => ({
    label: arrangor.navn,
    value: arrangor.id,
  }));
}
