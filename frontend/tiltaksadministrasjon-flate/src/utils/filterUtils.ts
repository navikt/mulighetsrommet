import {
  ArrangorDto,
  AvtaleStatusType,
  Avtaletype,
  GjennomforingStatusType,
  TilsagnStatus,
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

export const ENKELTPLASS_STATUS_OPTIONS: {
  label: string;
  value: GjennomforingStatusType;
}[] = [
  {
    label: "Utkast til påmelding",
    value: GjennomforingStatusType.ENKELTPLASS_UTKAST_TIL_PAMELDING,
  },
  {
    label: "Søkt inn",
    value: GjennomforingStatusType.ENKELTPLASS_SOKT_INN,
  },
  {
    label: "Venter på oppstart",
    value: GjennomforingStatusType.ENKELTPLASS_VENTER_PA_OPPSTART,
  },
  {
    label: "Deltar",
    value: GjennomforingStatusType.ENKELTPLASS_DELTAR,
  },
  {
    label: "Ikke aktuell",
    value: GjennomforingStatusType.ENKELTPLASS_IKKE_AKTUELL,
  },
  {
    label: "Fullført",
    value: GjennomforingStatusType.ENKELTPLASS_FULLFORT,
  },
  {
    label: "Avbrutt",
    value: GjennomforingStatusType.ENKELTPLASS_AVBRUTT,
  },
  {
    label: "Avbrutt utkast",
    value: GjennomforingStatusType.ENKELTPLASS_AVBRUTT_UTKAST,
  },
  {
    label: "Feilregistrert",
    value: GjennomforingStatusType.ENKELTPLASS_FEILREGISTRERT,
  },
];

export function gjennomforingStatusLabel(status: GjennomforingStatusType): string {
  return (
    [...TILTAKSGJENNOMFORING_STATUS_OPTIONS, ...ENKELTPLASS_STATUS_OPTIONS].find(
      (option) => option.value === status,
    )?.label ?? status
  );
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

export const TILSAGN_STATUS_OPTIONS: { label: string; value: TilsagnStatus }[] = [
  {
    label: "Til godkjenning",
    value: TilsagnStatus.TIL_GODKJENNING,
  },
  {
    label: "Godkjent",
    value: TilsagnStatus.GODKJENT,
  },
  {
    label: "Returnert",
    value: TilsagnStatus.RETURNERT,
  },
  {
    label: "Til annullering",
    value: TilsagnStatus.TIL_ANNULLERING,
  },
  {
    label: "Annullert",
    value: TilsagnStatus.ANNULLERT,
  },
  {
    label: "Til oppgjør",
    value: TilsagnStatus.TIL_OPPGJOR,
  },
  {
    label: "Oppgjort",
    value: TilsagnStatus.OPPGJORT,
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
