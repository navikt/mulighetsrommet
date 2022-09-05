export interface State {
  tekst: string;
  sendtStatus: Status;
  dialogId: string;
  malTekst: string;
}

export type Status = 'IKKE_SENDT' | 'SENDER' | 'SENDT_OK' | 'SENDING_FEILET';

export interface SEND_MELDING_ACTION {
  type: 'Send melding';
}

export interface AVBRYT_ACTION {
  type: 'Avbryt';
}

export interface SET_TEKST_ACTION {
  type: 'Sett tekst';
  payload: string;
}

export interface SENDT_OK_ACTION {
  type: 'Sendt ok';
  payload: string;
}

export interface SENDING_FEILET_ACTION {
  type: 'Sending feilet';
}

export interface RESET_ACTION {
  type: 'Reset';
}

export type Actions =
  | SEND_MELDING_ACTION
  | SET_TEKST_ACTION
  | AVBRYT_ACTION
  | SENDT_OK_ACTION
  | SENDING_FEILET_ACTION
  | RESET_ACTION;
