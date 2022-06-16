export interface Tiltakstype {
  _id: number;
  tiltakstypeNavn: string;
  beskrivelse?: string;
  innsatsgruppe: string;
  varighet?: string;
  regelverkFil?: string; //skal være fil
  regelverkFilNavn?: string;
  regelverkLenke?: string;
  regelverkLenkeNavn?: string;
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
}

export interface Tiltaksgjennomforing {
  _id: number;
  tiltakstype: Tiltakstype;
  tiltaksgjennomforingNavn: string;
  beskrivelse?: string;
  tiltaksnummer: number;
  kontaktinfoArrangor: Arrangor;
  lokasjon: string;
  oppstart: string;
  oppstartsdato?: string;
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
  kontaktinfoTiltaksansvarlig: Tiltaksansvarlig;
}

export interface Arrangor {
  _id: number;
  selskapsnavn: string;
  telefonnummer: string;
  epost: string;
  adresse: string;
}

export interface Tiltaksansvarlig {
  _id: number;
  navn: string;
  enhet: string;
  telefonnummer: string;
  epost: string;
}
