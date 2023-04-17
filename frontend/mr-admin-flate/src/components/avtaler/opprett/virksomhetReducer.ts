import { AmtVirksomhet } from "mulighetsrommet-api-client";

interface State {
  status: "idle" | "fetching" | "fetched";
  data: AmtVirksomhet | null;
}

type resetAction = {
  type: "Reset";
};

type hentDataAction = {
  type: "Hent data";
};

type dataHentetAction = {
  type: "Data hentet";
  payload: AmtVirksomhet;
};

type Actions = hentDataAction | dataHentetAction | resetAction;

export const initialState: State = {
  status: "idle",
  data: null,
};

export function reducer(state: State, action: Actions): State {
  switch (action.type) {
    case "Reset":
      return { ...state, status: "idle", data: null };
    case "Hent data":
      return { ...state, status: "fetching", data: null };
    case "Data hentet":
      return { ...state, status: "fetched", data: action.payload };
  }
}
