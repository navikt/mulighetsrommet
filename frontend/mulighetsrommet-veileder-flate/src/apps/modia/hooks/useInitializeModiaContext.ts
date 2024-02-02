import { useSetAtom } from "jotai";
import { useOverordnetEnhet } from "@/core/api/queries/useOverordnetEnhet";
import { modiaContextAtom, useModiaContext } from "./useModiaContext";
import { useEffect } from "react";
import { useLogEvent } from "@/logging/amplitude";

export function useInitializeModiaContext() {
  const appContext = useModiaContext();
  const setAppContext = useSetAtom(modiaContextAtom);
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
