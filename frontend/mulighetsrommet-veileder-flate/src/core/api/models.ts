import { SanityNokkelinfoKomponenter, SanityTiltakstype } from 'mulighetsrommet-api-client';

export type IndividuellTiltaksType =
  | 'ARBEIDSTRENING'
  | 'MIDLERTIDIG_LONNSTILSKUDD'
  | 'VARIG_LONNSTILSKUDD'
  | 'MENTOR'
  | 'INKLUDERINGSTILSKUDD'
  | 'SOMMERJOBB';

export type Tilgjengelighetsstatus = 'Ledig' | 'Venteliste' | 'Stengt';
export type Oppstart = 'dato' | 'lopende' | 'midlertidig_stengt';

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
  tiltakstype: SanityTiltakstype;
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

export interface StatistikkFil {
  _id: string;
  statistikkFilUrl: string;
}

export interface NokkelinfoKomponenter extends SanityNokkelinfoKomponenter {
  uuTitle?: string;
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
