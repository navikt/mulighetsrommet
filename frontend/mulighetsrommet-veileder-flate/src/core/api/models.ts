export type Tiltakstyper =
  | 'Digital jobbklubb'
  | 'AFT'
  | 'Jobbklubb'
  | 'ARR'
  | 'Oppfølging'
  | 'Avklaring'
  | 'VTA'
  | 'Opplæring (Gruppe AMO)'
  | 'Midlertidig lønnstilskudd';

type Innsatsgrupper =
  | 'Standard innsats'
  | 'Situasjonsbestemt innsats'
  | 'Spesielt tilpasset innsats'
  | 'Varig tilpasset innsats';

export type Tilgjengelighetsstatus = 'Ledig' | 'Venteliste' | 'Stengt';
export type Oppstart = 'dato' | 'lopende' | 'midlertidig_stengt';
type Tiltaksgruppe = 'individuelt' | 'gruppe';

export interface Tiltakstype {
  _id: string;
  tiltakstypeNavn: Tiltakstyper;
  tiltaksgruppe: Tiltaksgruppe;
  beskrivelse?: string;
  innsatsgruppe: Innsatsgruppe;
  varighet?: string;
  regelverkFiler?: RegelverkFil[];
  regelverkLenker?: RegelverkLenke[];
  regelverkLenkeNavn?: string;
  nokkelinfoKomponenter?: NokkelinfoKomponenter[];
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
  forskningsrapport?: Forskningsrapport[];
  delingMedBruker?: string;
}

export interface Forskningsrapport {
  _id: string;
  tittel: string;
  beskrivelse: any;
  lenker?: Lenke[];
}

interface Lenke {
  lenke: string;
  lenkenavn: string;
}

export interface Tiltaksgjennomforing {
  _id: string;
  tiltakstype: Tiltakstype;
  tiltaksgjennomforingNavn: string;
  beskrivelse?: string;
  tiltaksnummer: number;
  kontaktinfoArrangor?: Arrangor;
  lokasjon: string;
  oppstart: Oppstart;
  oppstartsdato?: string;
  estimert_ventetid?: string;
  faneinnhold?: {
    forHvemInfoboks?: string;
    forHvem?: object;
    detaljerOgInnholdInfoboks?: string;
    detaljerOgInnhold?: object;
    pameldingOgVarighetInfoboks?: string;
    pameldingOgVarighet?: object;
  };
  kontaktinfoTiltaksansvarlige: Tiltaksansvarlig[];
  tilgjengelighetsstatus?: Tilgjengelighetsstatus;
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
  tittel: Innsatsgrupper;
  nokkel: InnsatsgruppeNokler;
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

export interface NokkelinfoKomponenter {
  _id: string;
  tittel: string;
  innhold: string;
  hjelpetekst?: string;
}

export interface StatistikkFraCsvFil {
  År: string;
  'Antall Måneder': string;
  'Arbeidstaker m. ytelse/oppf': string;
  'Kun arbeidstaker': string;
  'Registrert hos Nav': string;
  Tiltakstype: string;
  Ukjent: string;
}

export type InnsatsgruppeNokler =
  | 'STANDARD_INNSATS'
  | 'SITUASJONSBESTEMT_INNSATS'
  | 'SPESIELT_TILPASSET_INNSATS'
  | 'VARIG_TILPASSET_INNSATS';
