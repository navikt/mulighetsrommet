import { PlusIcon } from "@navikt/aksel-icons";
import { Alert, Heading, Skeleton, VStack } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import styles from "./Landingsside.module.scss";
import { HistorikkKort } from "../historikk/HistorikkKort";
import { useHistorikkFraKomet } from "../../../core/api/queries/useHistorikkFraKomet";
import { UtkastKort } from "../historikk/UtkastKort";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../../utils/ErrorFallback";
import { Suspense } from "react";

function SkeletonLoader() {
  return <Skeleton variant="rounded" height={"10rem"} width={"40rem"} />;
}

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <VStack gap="10" className={styles.container}>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
          <Suspense fallback={<SkeletonLoader />}>
            <Utkast />
          </Suspense>
        </ErrorBoundary>
        <div>
          <Link className={styles.cta_link} to="/arbeidsmarkedstiltak/oversikt">
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </div>
        <ErrorBoundary FallbackComponent={ErrorFallback}>
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
