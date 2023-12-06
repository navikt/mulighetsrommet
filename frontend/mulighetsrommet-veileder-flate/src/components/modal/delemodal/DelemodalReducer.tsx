import { Actions, State } from "./DelemodalActions";
import { useLogEvent } from "../../../logging/amplitude";

export const logDelMedbrukerEvent = (
  action:
    | "Åpnet dialog"
    | "Delte med bruker"
    | "Del med bruker feilet"
    | "Avbrutt del med bruker"
    | "Endre deletekst"
    | "Sett venter på svar fra bruker",
) => {
  const { logEvent } = useLogEvent();
  logEvent({
    name: "arbeidsmarkedstiltak.del-med-bruker",
    data: { action },
  });
};

export function reducer(state: State, action: Actions): State {
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
    case "Lukk modal":
      return {
        ...state,
        modalOpen: action.payload,
      };
    case "Lukk statusmodal":
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

export function initInitialState(tekster: { deletekst: string }): State {
  return {
    originalDeletekst: tekster.deletekst,
    deletekst: tekster.deletekst,
    sendtStatus: "IKKE_SENDT",
    dialogId: "",
    venterPaaSvarFraBruker: false,
    enableRedigerDeletekst: false,
    modalOpen: false,
    statusmodalOpen: false,
  };
}
