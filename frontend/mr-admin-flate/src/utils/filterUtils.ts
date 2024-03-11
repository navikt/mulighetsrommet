import {
  Avtalestatus,
  LagretVirksomhet,
  NavEnhet,
  NavEnhetType,
  TiltaksgjennomforingStatus,
  Tiltakstype,
} from "mulighetsrommet-api-client";

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

export const regionOptions = (enheter: NavEnhet[]) => {
  return enheter
    .filter((enhet) => enhet.type === NavEnhetType.FYLKE)
    .sort()
    .map((enhet) => ({
      label: enhet.navn,
      value: enhet.enhetsnummer,
    }));
};

export const enhetOptions = (enheter: NavEnhet[], navRegioner: string[]) => {
  return enheter
    .filter((enhet) => {
      const erLokalEllerTiltaksenhet =
        enhet.type === NavEnhetType.LOKAL || enhet.type === NavEnhetType.TILTAK;
      const enheterFraFylke =
        navRegioner.length === 0 ? true : navRegioner.includes(enhet.overordnetEnhet ?? "");
      return erLokalEllerTiltaksenhet && enheterFraFylke;
    })
    .sort()
    .map((enhet) => ({
      label: `${enhet.navn} - ${enhet.enhetsnummer}`,
      value: enhet.enhetsnummer,
    }));
};

export const tiltakstypeOptions = (tiltakstyper: Tiltakstype[]) => {
  return (
    tiltakstyper.sort().map((tiltakstype) => ({
      label: tiltakstype.navn,
      value: tiltakstype.id,
    })) || []
  );
};

export const virksomhetOptions = (virksomheter: LagretVirksomhet[]) => {
  return virksomheter.sort().map((virksomhet) => ({
    label: virksomhet.navn,
    value: virksomhet.organisasjonsnummer,
  }));
};
