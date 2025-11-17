import {
  ArrangorflateArrangor,
  ArrangorflateGjennomforing,
  DataDrivenTableDto,
  DataDrivenTableDtoColumnAlign,
  DataElementTextFormat,
  Tiltakskode,
} from "@api-client";
import { dataElementLink, dataElementText } from "../dataDrivenTableHelpers";

export const gjennomforingIdAFT = "54d0d2af-f329-480d-a427-30de446fea10";
export const gjennomforingIdAvklaring = "54d0d2af-f329-480d-a427-30de446fea11";
export const gjennomforingIdOppfolging = "54d0d2af-f329-480d-a427-30de446fea12";

const today: Date = new Date();

export const arrangorMock: ArrangorflateArrangor = {
  id: "cc04c391-d733-4762-8208-b0dd4387a126",
  navn: "Arrangørens navn",
  organisasjonsnummer: "123456789",
};

export const gjennomforingAFT: ArrangorflateGjennomforing = {
  id: gjennomforingIdAFT,
  navn: "Et AFT-tiltak Investering",
  tiltakstype: {
    navn: "Arbeidsforberedende trening",
    tiltakskode: Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
  },
  startDato: new Date(today.getFullYear() - 5, 7, 1).toISOString().slice(0, 8),
  sluttDato: null,
};

export const gjennomforingAvklaring: ArrangorflateGjennomforing = {
  id: gjennomforingIdAvklaring,
  navn: "Et avklaringstiltak med annen avtalt pris",
  tiltakstype: {
    navn: "Avklaring",
    tiltakskode: Tiltakskode.AVKLARING,
  },
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 8),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 8),
};

export const gjennomforingOppfolging: ArrangorflateGjennomforing = {
  id: gjennomforingIdOppfolging,
  navn: "Et oppfølgingstiltak med avtalt timespris",
  tiltakstype: {
    navn: "Oppfølging",
    tiltakskode: Tiltakskode.OPPFOLGING,
  },
  startDato: new Date(today.getFullYear() - 1, 1, 1).toISOString().slice(0, 8),
  sluttDato: new Date(today.getFullYear() + 1, 11, 31).toISOString().slice(0, 8),
};

export const oversiktAktiveGjennomforinger: DataDrivenTableDto = {
  columns: [
    { key: "navn", label: "Navn", sortable: true, align: DataDrivenTableDtoColumnAlign.LEFT },
    {
      key: "tiltaksType",
      label: "Type",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "startDato",
      label: "Startdato",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    {
      key: "sluttDato",
      label: "Sluttdato",
      sortable: true,
      align: DataDrivenTableDtoColumnAlign.LEFT,
    },
    { key: "action", label: null, sortable: false, align: DataDrivenTableDtoColumnAlign.CENTER },
  ],
  rows: [gjennomforingAFT, gjennomforingAvklaring, gjennomforingOppfolging].map(
    (gjennomforing) => ({
      cells: {
        navn: dataElementText(gjennomforing.navn),
        tiltaksType: dataElementText(gjennomforing.tiltakstype.navn),
        startDato: dataElementText(gjennomforing.startDato, DataElementTextFormat.DATE),
        sluttDato: dataElementText(gjennomforing.sluttDato ?? "", DataElementTextFormat.DATE),
        action: dataElementLink(
          "Start innsending",
          `/123456789/opprett-krav/${gjennomforing.id}/innsendingsinformasjon`,
        ),
      },
      content: null,
    }),
  ),
};
