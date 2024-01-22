import { atom, useAtom } from "jotai";

export interface AppContextData {
  fnr: string;
  enhet: string;
  overordnetEnhet: string | null | undefined;
}

export const appContextAtom = atom<Partial<AppContextData>>({});

export function useAppContext(): AppContextData {
  const [data] = useAtom(appContextAtom);

  if (data == null) {
    throw Error("Missing data in AppContext");
  }

  const { fnr, enhet, overordnetEnhet } = data;

  if (!fnr) {
    throw Error("Missing fnr in AppContext");
  }

  if (!enhet) {
    throw Error("Missing enhet in AppContext");
  }

  return { fnr, enhet, overordnetEnhet };
}
