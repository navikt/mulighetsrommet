import { useSetAtom } from "jotai";
import { useOverordnetEnhet } from "@/api/queries/useOverordnetEnhet";
import { modiaContextAtom, useModiaContext } from "./useModiaContext";
import { useEffect } from "react";
import { useLogEvent } from "@/logging/amplitude";

export function useInitializeModiaContext() {
  const appContext = useModiaContext();
  const setAppContext = useSetAtom(modiaContextAtom);
  const { data: overordnetEnhet } = useOverordnetEnhet(appContext.enhet);
  const { logEvent } = useLogEvent();

  const enhetsnrForOverordnetEnhet = overordnetEnhet?.enhetsnummer;
  useEffect(() => {
    if (enhetsnrForOverordnetEnhet) {
      setAppContext({ ...appContext, overordnetEnhet: enhetsnrForOverordnetEnhet });
    }
  }, [enhetsnrForOverordnetEnhet, setAppContext, appContext]);

  useEffect(() => {
    if (appContext.overordnetEnhet) {
      logEvent({ name: "arbeidsmarkedstiltak.unike-brukere" });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appContext.overordnetEnhet]);
}
