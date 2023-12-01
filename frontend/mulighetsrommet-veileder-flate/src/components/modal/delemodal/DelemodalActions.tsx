export interface State {
  deletekst: string;
  originalHilsen: string;
  introtekst: string;
  hilsen: string;
  sendtStatus: Status;
  dialogId: string;
  skrivPersonligMelding: boolean;
  skrivPersonligIntro: boolean;
  venterPaaSvarFraBruker: boolean;
}

export type Status = "IKKE_SENDT" | "SENDER" | "SENDT_OK" | "SENDING_FEILET";

export interface SEND_MELDING_ACTION {
  type: "Send melding";
}

export interface AVBRYT_ACTION {
  type: "Avbryt";
  payload: { tekster: { introtekst: string; deletekst: string; originalHilsen: string } };
}

export interface SENDT_OK_ACTION {
  type: "Sendt ok";
  payload: string;
}

export interface SENDING_FEILET_ACTION {
  type: "Sending feilet";
}

export interface RESET_ACTION {
  type: "Reset";
}

export interface SETT_HILSEN_ACTION {
  type: "Sett hilsen";
  payload: string;
}

export interface SETT_INTRO_ACTION {
  type: "Sett intro";
  payload: string;
}

export interface SKRIV_PERSONLIG_MELDING {
  type: "Skriv personlig melding";
  payload: boolean;
}

export interface SKRIV_PERSONLIG_INTRO {
  type: "Skriv personlig intro";
  payload: boolean;
}

export interface SETT_VENTER_PAA_SVAR_FRA_BRUKER {
  type: "Venter på svar fra bruker";
  payload: boolean;
}

export type Actions =
  | SEND_MELDING_ACTION
  | AVBRYT_ACTION
  | SENDT_OK_ACTION
  | RESET_ACTION
  | SENDING_FEILET_ACTION
  | SETT_HILSEN_ACTION
  | SETT_INTRO_ACTION
  | SKRIV_PERSONLIG_MELDING
  | SKRIV_PERSONLIG_INTRO
  | SETT_VENTER_PAA_SVAR_FRA_BRUKER;
