export interface Tiltakstype {
  _id: number;
  tiltakstypeNavn: string;
  beskrivelse?: string;
  innsatsgruppe: Innsatsgruppe;
  varighet?: string;
  regelverkFiler: RegelverkFil[];
  regelverkLenker: RegelverkLenke[];
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
  kontaktinfoTiltaksansvarlige: Tiltaksansvarlig[];
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

export interface Innsatsgruppe {
  _id: string;
  beskrivelse: string;
  tittel: string;
}

export interface RegelverkFil {
  _id: string;
  regelverkFilUrl: string;
  regelverkFilNavn: string;
}

export interface RegelverkLenke {
  _id: string;
  regelverkUrl: string;
  regelverkLenkeNavn: string;
}
