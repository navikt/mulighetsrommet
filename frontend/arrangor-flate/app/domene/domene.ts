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
}
