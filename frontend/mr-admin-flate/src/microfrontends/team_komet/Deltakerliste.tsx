import React from "react";
import { ErrorBoundary } from "react-error-boundary";
import { getEnvironment } from "../../api/getEnvironment";
import { useManifest } from "../../api/useManifest";
import { Laster } from "../../components/laster/Laster";
import { deltakerlisteKometManifestUrl, DELTAKERLISTE_KOMET } from "../../urls";
import { DELTAKERLISTE_ENTRY, DELTAKERLISTE_MODULE } from "../entrypoints";
import { Alert, Button } from "@navikt/ds-react";

export function DeltakerListe() {
  const { data: manifest, isLoading: isLoadingManifest } = useManifest(
    deltakerlisteKometManifestUrl
  );
  if (isLoadingManifest) {
    return <Laster />;
  }

  const DeltakerlisteMikrofrontend = React.lazy(
    () =>
      import(
        `${DELTAKERLISTE_KOMET[getEnvironment()]}/${
          manifest[DELTAKERLISTE_ENTRY][DELTAKERLISTE_MODULE]
        }`
      )
  );

  return (
    <React.Suspense fallback="Laster...">
      <ErrorBoundary
        FallbackComponent={({ resetErrorBoundary }) => {
          return (
            <>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "1rem",
                }}
              >
                <Alert variant="error">Klarte ikke laste deltakerliste</Alert>
                <Button onClick={resetErrorBoundary}>Prøv på nytt</Button>
              </div>
            </>
          );
        }}
      >
        <DeltakerlisteMikrofrontend />
      </ErrorBoundary>
    </React.Suspense>
  );
}
