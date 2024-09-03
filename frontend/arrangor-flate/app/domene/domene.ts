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
