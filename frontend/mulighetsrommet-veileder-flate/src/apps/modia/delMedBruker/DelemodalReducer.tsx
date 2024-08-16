import { Actions, State } from "./DelemodalActions";
import { useReducer } from "react";

export function useDelMedBruker(deletekst: string) {
  return useReducer(reducer, { deletekst }, initInitialState);
}

function reducer(state: State, action: Actions): State {
  switch (action.type) {
    case "Avbryt":
      return {
        ...state,
        deletekst: state.originalDeletekst,
        sendtStatus: "IKKE_SENDT",
        venterPaaSvarFraBruker: false,
        enableRedigerDeletekst: false,
        modalOpen: false,
      };
    case "Send melding":
      return { ...state, sendtStatus: "SENDER" };
    case "Sendt ok":
      return {
        ...state,
        deletekst: state.originalDeletekst,
        sendtStatus: "SENDT_OK",
        dialogId: action.payload,
        enableRedigerDeletekst: false,
        modalOpen: false,
        statusmodalOpen: true,
      };
    case "Sending feilet":
      return {
        ...state,
        sendtStatus: "SENDING_FEILET",
        enableRedigerDeletekst: false,
        modalOpen: false,
        statusmodalOpen: true,
      };
    case "Venter på svar fra bruker": {
      return { ...state, venterPaaSvarFraBruker: action.payload };
    }
    case "Set deletekst":
      return {
        ...state,
        deletekst: action.payload,
      };
    case "Enable rediger deletekst":
      return {
        ...state,
        enableRedigerDeletekst: action.payload,
      };
    case "Toggle modal":
      return {
        ...state,
        modalOpen: action.payload,
      };
    case "Toggle statusmodal":
      return {
        ...state,
        statusmodalOpen: action.payload,
      };
    case "Reset":
      return initInitialState({
        deletekst: state.originalDeletekst,
      });
  }
}

function initInitialState(tekster: { deletekst: string }): State {
  return {
    originalDeletekst: tekster.deletekst,
    deletekst: tekster.deletekst,
    sendtStatus: "IKKE_SENDT",
    dialogId: null,
    venterPaaSvarFraBruker: false,
    enableRedigerDeletekst: false,
    modalOpen: false,
    statusmodalOpen: false,
  };
}
