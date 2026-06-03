export interface State {
  deletekst: string;
  originalDeletekst: string;
  sendtStatus: Status;
  dialogId: string | null;
  venterPaaSvarFraBruker: boolean;
  enableRedigerDeletekst: boolean;
  modalOpen: boolean;
  statusmodalOpen: boolean;
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

export interface SET_VENTER_PAA_SVAR_FRA_BRUKER {
  type: "Venter på svar fra bruker";
  payload: boolean;
}

export interface SET_DELETEKST_ACTION {
  type: "Set deletekst";
  payload: string;
}

export interface ENABLE_REDIGER_DELETEKST_ACTION {
  type: "Enable rediger deletekst";
  payload: boolean;
}

export interface LUKK_MODAL_ACTION {
  type: "Toggle modal";
  payload: boolean;
}

export interface LUKK_STATUSMODAL_ACTION {
  type: "Toggle statusmodal";
  payload: boolean;
}

export type Actions =
  | SEND_MELDING_ACTION
  | AVBRYT_ACTION
  | SENDT_OK_ACTION
  | RESET_ACTION
  | SENDING_FEILET_ACTION
  | SET_VENTER_PAA_SVAR_FRA_BRUKER
  | SET_DELETEKST_ACTION
  | ENABLE_REDIGER_DELETEKST_ACTION
  | LUKK_MODAL_ACTION
  | LUKK_STATUSMODAL_ACTION;
