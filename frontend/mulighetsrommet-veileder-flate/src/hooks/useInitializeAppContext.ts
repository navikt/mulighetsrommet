import { useSetAtom } from "jotai";
import { useOverordnetEnhet } from "../core/api/queries/useOverordnetEnhet";
import { appContext } from "../core/atoms/atoms";
import { AppContextData, useAppContext } from "./useAppContext";
import { useEffect } from "react";

export function useInitializeAppContext(): AppContextData {
  const setContextData = useSetAtom(appContext);
  const contextData = useAppContext();
  const { data } = useOverordnetEnhet(contextData.enhet);

  useEffect(() => {
    setContextData({ ...contextData, overordnetEnhet: data?.enhetsnummer });
  }, [data]);

  return contextData;
}
