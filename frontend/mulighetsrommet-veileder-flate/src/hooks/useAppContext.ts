import { useAtom } from "jotai";
import { appContext } from "../core/atoms/atoms";

export interface AppContextData {
  fnr: string;
  enhet: string;
  overordnetEnhet: string | null | undefined;
}

export function useAppContext(): AppContextData {
  const [data] = useAtom(appContext);

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
