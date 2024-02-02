import { useSuspenseQuery } from "@tanstack/react-query";
import { environment } from "@/environment";
import { headers } from "@/core/api/headers";

export const DELTAKERREGISTRERING_ENTRY = "src/webComponentWrapper.tsx";

const DELTAKERREGISTRERING_KOMET = {
  LOCAL: "http://localhost:4173",
  DEV: "https://amt-deltaker-flate.intern.dev.nav.no", // URL til bundle som blir hostet et sted i dev
  PROD: "", // URL til bundle som blir hostet et sted i prod
};

const deltakerRegistreringOrigin = DELTAKERREGISTRERING_KOMET[environment];

const deltakerregistreringKometManifestUrl = `${deltakerRegistreringOrigin}/asset-manifest.json`;

interface DeltakerRegistreringAssetManifest {
  "src/webComponentWrapper.tsx": {
    file: string;
  };
}

export function useLoadDeltakerRegistreringApp() {
  return useSuspenseQuery<any>({
    queryKey: ["deltaker-registrering-asset-manifest"],
    queryFn: async () => {
      const response = await fetch(deltakerregistreringKometManifestUrl, {
        headers,
      });

      if (!response.ok) {
        throw new Error("Failed to load DeltakerRegistrering");
      }

      const manifest: DeltakerRegistreringAssetManifest = await response.json();
      const entry = manifest[DELTAKERREGISTRERING_ENTRY].file;
      return import(/* @vite-ignore */ `${deltakerRegistreringOrigin}/${entry}`);
    },
  });
}
