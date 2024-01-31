import { Alert, Button } from "@navikt/ds-react";
import React from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useLoadDeltakerRegistreringApp } from "@/microfrontends/deltaker-registrering/useLoadDeltakerRegistreringApp";
import { useGetTiltaksgjennomforingIdFraUrl } from "@/core/api/queries/useGetTiltaksgjennomforingIdFraUrl";
import { useAppContext } from "@/hooks/useAppContext";

export function DeltakerRegistrering() {
  return (
    <React.Suspense fallback="Laster...">
      <ErrorBoundary
        FallbackComponent={({ resetErrorBoundary }) => {
          return (
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
          );
        }}
      >
        <DeltakerRegistreringApp />
      </ErrorBoundary>
    </React.Suspense>
  );
}

function DeltakerRegistreringApp() {
  useLoadDeltakerRegistreringApp();

  const tiltaksgjennomforingId = useGetTiltaksgjennomforingIdFraUrl();

  const { fnr, enhet } = useAppContext();

  return React.createElement("arbeidsmarkedstiltak-deltaker", {
    "data-personident": fnr,
    "data-deltakerlisteId": tiltaksgjennomforingId,
    "data-enhetId": enhet,
  });
}
