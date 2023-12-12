import { useSuspenseQuery } from "@tanstack/react-query";
import {
  DELTAKERREGISTRERING_ENTRY,
  DELTAKERREGISTRERING_MODULE,
} from "../../microfrontends/entrypoints";
import { DELTAKERREGISTRERING_KOMET } from "../../urls";
import { getEnvironment } from "./getEnvironment";

export function useLoadDeltakerRegistreringApp(manifestUrl: string) {
  return useSuspenseQuery<any>({
    queryKey: ["manifest"],
    queryFn: async () => {
      const response = await fetch(manifestUrl);
      if (!response.ok) {
        throw new Error("Could not fetch");
      }

      const manifest = await response.json();
      return import(
        /* @vite-ignore */
        `${DELTAKERREGISTRERING_KOMET[getEnvironment()]}/${
          manifest[DELTAKERREGISTRERING_ENTRY][DELTAKERREGISTRERING_MODULE]
        }`
      );
    },
  });
}
