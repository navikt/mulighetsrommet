type SETT_FEIL_ACTION = {
  type: "Sett feil";
  payload: string;
};

type OPPRETT_TAG_ACTION = {
  type: "Opprett tag";
};

type TAG_OPPRETTET_ACTION = {
  type: "Tag opprettet";
};

type Actions = SETT_FEIL_ACTION | OPPRETT_TAG_ACTION | TAG_OPPRETTET_ACTION;

interface State {
  isLoading: boolean;
  error?: string;
}

export const initialTagState: State = {
  isLoading: false,
  error: undefined,
};

export function reducer(state: State, action: Actions): State {
  switch (action.type) {
    case "Opprett tag":
      return { ...state, isLoading: true, error: "" };
    case "Tag opprettet":
      return { ...state, isLoading: false, error: "" };
    case "Sett feil":
      return { ...state, error: action.payload };
  }
}
