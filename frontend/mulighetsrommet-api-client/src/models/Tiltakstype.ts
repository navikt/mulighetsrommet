/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type Tiltakstype = {
  _id: number;
  tiltakstypeNavn: string;
  beskrivelse?: string;
  innsatsgruppe: string;
  varighet?: string;
  regelverkFil?: string; //skal være fil
  regelverkLenke?: string;
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
};
