import { useSetAtom } from "jotai";
import { useOverordnetEnhet } from "@/api/queries/useOverordnetEnhet";
import { modiaContextAtom, useModiaContext } from "./useModiaContext";
import { useEffect } from "react";

export function useInitializeModiaContext() {
  const appContext = useModiaContext();
  const setAppContext = useSetAtom(modiaContextAtom);
  const { data: overordnetEnhet } = useOverordnetEnhet(appContext.enhet);

  const enhetsnrForOverordnetEnhet = overordnetEnhet?.enhetsnummer;
  useEffect(() => {
    if (enhetsnrForOverordnetEnhet) {
      setAppContext({ ...appContext, overordnetEnhet: enhetsnrForOverordnetEnhet });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enhetsnrForOverordnetEnhet]);
}
