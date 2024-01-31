import { useSetAtom } from "jotai";
import { useOverordnetEnhet } from "../core/api/queries/useOverordnetEnhet";
import { appContextAtom, useAppContext } from "./useAppContext";
import { useEffect } from "react";
import { useLogEvent } from "../logging/amplitude";

export function useInitializeAppContext() {
  const appContext = useAppContext();
  const setAppContext = useSetAtom(appContextAtom);
  const { data: overordnetEnhet } = useOverordnetEnhet(appContext.enhet);
  const { logEvent } = useLogEvent();

  useEffect(() => {
    if (overordnetEnhet?.enhetsnummer) {
      setAppContext({ ...appContext, overordnetEnhet: overordnetEnhet.enhetsnummer });
    }
  }, [overordnetEnhet?.enhetsnummer]);

  useEffect(() => {
    if (appContext.overordnetEnhet) {
      logEvent({ name: "arbeidsmarkedstiltak.unike-brukere" });
    }
  }, [appContext.overordnetEnhet]);
}
