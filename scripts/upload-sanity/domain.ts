export interface SanityKontaktperson {
  _id: string;
  _type: "navKontaktperson";
  navn: string;
  enhet: string;
  telefonnummer: string;
  ident?: string;
  epost: string;
}

export interface SanityArrangor {
  _type: "arrangor";
  _id: string;
  selskapsnavn: string;
  telefonnummer: string;
  epost?: string;
  adresse: string;
}

export interface SanityTiltaksgjennomforing {
  _id: string;
  _type: "tiltaksgjennomforing";
  tiltakstype?: Reference;
  tiltaksgjennomforingNavn: string;
  beskrivelse: string;
  tiltaksnummer: number;
  kontaktinfoArrangor: Reference;
  lokasjon: string;
  fylke: Reference;
  enheter: Reference[];
  oppstart: string;
  oppstartsdato: string;
  faneinnhold: {
    _type: "object";
    forHvemInfoboks: string;
    forHvem: Block[];
    detaljerOgInnholdInfoboks: string;
    detaljerOgInnhold: Block[];
    pameldingOgVarighetInfoboks: string;
    pameldingOgVarighet: Block[];
  };
  kontaktinfoTiltaksansvarlige: Reference[];
  lenker?: Lenke[];
}

export interface Lenke {
  _key: string;
  lenke: string;
  lenkenavn: string;
}

export interface SanityTiltakstype {
  _id: string;
  _type: "tiltakstype";
  beskrivelse: Block[];
  faneinnhold: {
    _type: "object";
    detaljerOgInnhold: Block[];
    detaljerOgInnholdInfoboks: string;
    forHvem: Block[];
    forHvemInfoboks: string;
    innsikt: Block[];
    pameldingOgVarighet: Block[];
    pameldingOgVarighetInfoboks: string;
  };
  innsatsgruppe: Innsatsgruppe;
  overgangTilArbeid: Block[];
  tiltakstypeNavn: Tiltakstype;
  varighet: string;
}

type Innsatsgruppe =
  | "Standardinnsats"
  | "Situasjonsbestemt innsats"
  | "Spesielt tilpasset innsats"
  | "Varig tilpasset innsats"
  | "";

export type Tiltakstype =
  | "Oppfølging"
  | "Avklaring"
  | "Jobbklubb (uten om digital jobbklubb)"
  | "Digital jobbklubb"
  | "ARR"
  | "AFT"
  | "VTA"
  | "Opplæring (Gruppe-AMO)"
  | "Opplæring (AMO Forhåndsgodkjent avtale)";

export interface Reference {
  _ref: string;
  _type: "reference";
  _key?: string;
}

export interface Block {
  _type: "block";
  _key: string;
  children: {
    _type: "span";
    _key: string;
    marks: [];
    text: string;
  }[];
  markDefs: [];
  style: "normal";
}

export interface SanityEnhet {
  _id: string;
  _type: "enhet";
  navn: string;
  status: "Aktiv" | "Nedlagt" | "Under utvikling" | "Under avvikling";
  type: "Fylke" | "Lokal";
}
