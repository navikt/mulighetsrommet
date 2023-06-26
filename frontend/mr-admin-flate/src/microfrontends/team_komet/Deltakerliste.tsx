import React from "react";
import { ErrorBoundary } from "react-error-boundary";
import { getEnvironment } from "../../api/getEnvironment";
import { useManifest } from "../../api/useManifest";
import { Laster } from "../../components/laster/Laster";
import { pocManifestUrl, POC_BASE_URL } from "../../urls";
import { DELTAKERLISTE_ENTRY, DELTAKERLISTE_MODULE } from "../entrypoints";

export function DeltakerListe() {
  const { data: manifest, isLoading: isLoadingManifest } =
    useManifest(pocManifestUrl);
  if (isLoadingManifest) {
    return <Laster />;
  }

  const DeltakerlisteMikrofrontend = React.lazy(
    () =>
      import(
        `${POC_BASE_URL[getEnvironment()]}/${
          manifest[DELTAKERLISTE_ENTRY][DELTAKERLISTE_MODULE]
        }`
      )
  );

  return (
    <React.Suspense fallback="Laster...">
      <ErrorBoundary
        FallbackComponent={() => <p>Klarte ikke laste inn deltakerlisten</p>}
      >
        <DeltakerlisteMikrofrontend />
      </ErrorBoundary>
    </React.Suspense>
  );
}
