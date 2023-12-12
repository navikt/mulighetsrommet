import { Alert, Button } from "@navikt/ds-react";
import React from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useLoadDeltakerRegistreringApp } from "../../core/api/useLoadDeltakerRegistreringApp";
import { deltakerregistreringKometManifestUrl } from "../../urls";

interface Props {
  fnr: string;
  deltakerliste: string;
}

export function DeltakerRegistrering(props: Props) {
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
                <Alert variant="error">Klarte ikke laste deltakerregistrering</Alert>
                <Button onClick={resetErrorBoundary}>Prøv på nytt</Button>
              </div>
            </>
          );
        }}
      >
        <DeltakerRegistreringKomponent {...props} />
      </ErrorBoundary>
    </React.Suspense>
  );
}

function DeltakerRegistreringKomponent({ fnr, deltakerliste }: Props) {
  useLoadDeltakerRegistreringApp(deltakerregistreringKometManifestUrl);

  return React.createElement("arbeidsmarkedstiltak-deltaker", {
    "data-personident": fnr,
    "data-deltakerlisteId": deltakerliste,
  });
}
