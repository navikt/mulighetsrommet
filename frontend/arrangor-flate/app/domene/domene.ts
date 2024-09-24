export interface Krav {
  id: string;
  tiltaksnr: string;
  kravnr: string;
  periode: string;
  belop: string;
  fristForGodkjenning: string;
  status: KravStatus;
}

export enum KravStatus {
  Attestert,
  KlarForInnsending,
  NarmerSegFrist,
}

export interface Deltakerliste {
  id: string;
  detaljer: {
    tiltaksnavn: string;
    tiltaksnummer: string;
    avtalenavn: string;
    tiltakstype: string;
    refusjonskravperiode: string;
    refusjonskravnummer: string;
  };
  deltakere: Deltaker[];
}

type Deltaker = {
  navn: string;
  veileder: string;
  fodselsdato: string;
  startDatoTiltaket: string;
  startDatoPerioden: string;
  sluttDatoPerioden: string;
  deltakelsesProsent: number;
  maanedsverk: number;
  belop: number;
};

export interface RolleTilgangRequest {
  personident: string;
}

export interface Rolletilgang {
  roller: TiltaksarrangorRoller[];
}

interface TiltaksarrangorRoller {
  organisasjonsnummer: string;
  roller: RolleType[];
}

export enum RolleType {
  "TILTAK_ARRANGOR_REFUSJON",
}
