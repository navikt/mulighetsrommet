import { useAtom } from "jotai";
import { useOverordnetEnhet } from "../core/api/queries/useOverordnetEnhet";
import { appContext } from "../core/atoms/atoms";
import { useAppContext } from "./useAppContext";
import { useEffect } from "react";

export function useUpdateAppContext() {
  const { enhet } = useAppContext();
  const { data } = useOverordnetEnhet(enhet);
  const [contextData, setContextData] = useAtom(appContext);

  useEffect(() => {
    setContextData({ ...contextData, overordnetEnhet: data?.enhetsnummer });
  }, [data]);
}
