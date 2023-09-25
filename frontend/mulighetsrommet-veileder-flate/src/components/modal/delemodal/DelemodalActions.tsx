export interface State {
  deletekst: string;
  originalHilsen: string;
  hilsen: string;
  sendtStatus: Status;
  dialogId: string;
}

export type Status = "IKKE_SENDT" | "SENDER" | "SENDT_OK" | "SENDING_FEILET";

export interface SEND_MELDING_ACTION {
  type: "Send melding";
}

export interface AVBRYT_ACTION {
  type: "Avbryt";
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

export type Actions =
  | SEND_MELDING_ACTION
  | AVBRYT_ACTION
  | SENDT_OK_ACTION
  | RESET_ACTION
  | SENDING_FEILET_ACTION
  | SETT_HILSEN_ACTION;
