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
