import { PlusIcon } from "@navikt/aksel-icons";
import { Alert, Heading, Skeleton, VStack } from "@navikt/ds-react";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Link } from "react-router-dom";
import { useHistorikkFraKomet } from "../../../core/api/queries/useHistorikkFraKomet";
import { HistorikkKort } from "../historikk/HistorikkKort";
import { UtkastKort } from "../historikk/UtkastKort";
import styles from "./Landingsside.module.scss";

function SkeletonLoader() {
  return <Skeleton variant="rounded" height={"10rem"} width={"40rem"} />;
}

function Feilmelding({ message }: { message: string }) {
  return (
    <Alert aria-live="polite" variant="error">
      {message}
    </Alert>
  );
}

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <VStack gap="10" className={styles.container}>
        <ErrorBoundary
          FallbackComponent={() => Feilmelding({ message: "Klarte ikke hente utkast for bruker" })}
        >
          <Suspense fallback={<SkeletonLoader />}>
            <Utkast />
          </Suspense>
        </ErrorBoundary>
        <div>
          <Link className={styles.cta_link} to="/arbeidsmarkedstiltak/oversikt">
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </div>
        <ErrorBoundary
          FallbackComponent={() =>
            Feilmelding({ message: "Klarte ikke hente historikk for bruker" })
          }
        >
          <Suspense
            fallback={
              <VStack gap="5">
                <SkeletonLoader />
                <SkeletonLoader />
              </VStack>
            }
          >
            <Historikk />
          </Suspense>
        </ErrorBoundary>
      </VStack>
    </main>
  );
}

function Historikk() {
  const { data } = useHistorikkFraKomet();
  if (!data) {
    return null;
  }

  const { historikk } = data;

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Historikk
      </Heading>
      {historikk.length > 0 ? (
        historikk.map((hist) => {
          return <HistorikkKort key={hist.deltakerId} historikk={hist} />;
        })
      ) : (
        <Alert variant="info">Ingen historikk for bruker</Alert>
      )}
    </VStack>
  );
}

function Utkast() {
  const { data } = useHistorikkFraKomet();
  if (!data) {
    return null;
  }

  const { aktive } = data;

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Utkast
      </Heading>

      {aktive.length > 0 ? (
        aktive.map((utkast) => {
          return <UtkastKort key={utkast.deltakerId} utkast={utkast} />;
        })
      ) : (
        <Alert variant="info">Ingen utkast for bruker</Alert>
      )}
    </VStack>
  );
}
