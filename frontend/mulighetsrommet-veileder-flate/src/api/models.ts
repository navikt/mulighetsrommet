type Tiltakstyper =
  | 'Digital jobbklubb'
  | 'AFT'
  | 'Jobbklubb'
  | 'ARR'
  | 'Oppfølging'
  | 'Avklaring'
  | 'VTA'
  | 'Opplæring (Gruppe AMO)';

export interface Tiltakstype {
  _id: string;
  tiltakstypeNavn: Tiltakstyper;
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
  statistikkKomponent: StatistikkKomponent[];
}

export interface Tiltaksgjennomforing {
  _id: string;
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
  _id: string;
  selskapsnavn: string;
  telefonnummer: string;
  epost: string;
  adresse: string;
}

export interface Tiltaksansvarlig {
  _id: string;
  navn: string;
  enhet: string;
  telefonnummer: string;
  epost: string;
}

export interface Innsatsgruppe {
  _id: string;
  beskrivelse: string;
  tittel: string;
  nokkel: string;
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

export interface StatistikkFil {
  _id: string;
  statistikkFilUrl: string;
}

export interface StatistikkKomponent {
  _id: string;
  statistikkOverskrift: string;
  statistikkInnhold: string;
  statistikkHjelpetekst: string;
}
