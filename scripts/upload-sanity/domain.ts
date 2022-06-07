export interface SanityKontaktperson {
  _id: string;
  _type: "navKontaktperson";
  navn: string;
  enhet: string;
  telefonnummer: string;
  epost: string;
  ident?: string;
}

export interface SanityArrangor {
  _type: "arrangor";
  _id: string;
  selskapsnavn: string;
  telefonnummer: string;
  epost: string;
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
  enheter?: {
    _type: "document";
    fylke: string;
    [x: string]: string; // TODO Finn ut av denne
  };
  oppstart: string;
  oppstartsdato: string;
  faneinnhold: {
    _type: "document";
    forHvemInfoboks: string;
    forHvem: Block[];
    detaljerOgInnholdInfoboks: string;
    detaljerOgInnhold: Block[];
    pameldingOgVarighetInfoboks: string;
    pameldingOgVarighet: Block[];
  };
  kontaktinfoTiltaksansvarlig: Reference;
}

export interface SanityTiltakstype {
  _id: string;
  _type: "tiltakstype";
  beskrivelse: Block[];
  faneinnhold: {
    _type: "document";
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

interface Reference {
  _ref: string;
  _type: "reference";
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
