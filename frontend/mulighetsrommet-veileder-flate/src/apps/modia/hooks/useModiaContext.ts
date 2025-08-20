import { atom, useAtom } from "jotai";

export interface AppContextData {
  fnr: string;
  enhet: string;
}

export const modiaContextAtom = atom<Partial<AppContextData>>({});

export function useModiaContext(): AppContextData {
  const [data] = useAtom(modiaContextAtom);

  const { fnr, enhet } = data;

  if (!fnr) {
    throw Error("Missing fnr in AppContext");
  }

  if (!enhet) {
    throw Error("Missing enhet in AppContext");
  }

  return { fnr, enhet };
}
