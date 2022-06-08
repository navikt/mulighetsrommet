/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import { Tiltakstype } from "./Tiltakstype";
import { Arrangor } from "./Arrangor";
import { Tiltaksansvarlig } from "./Tiltaksansvarlig";

export type Tiltaksgjennomforing = {
  _id: number;
  tiltakstype: Tiltakstype;
  tiltaksgjennomforingNavn: string;
  beskrivelse?: string;
  tiltaksnummer: number;
  kontaktinfoArrangor: Arrangor;
  lokasjon: string;
  enheter: { fylke: string };
  oppstart: string;
  oppstartsdato?: Date;
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
  kontaktinfoTiltaksansvarlig: Tiltaksansvarlig;
};
