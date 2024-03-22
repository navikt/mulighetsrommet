import { InformationSquareFillIcon, PlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Heading, Skeleton, VStack } from "@navikt/ds-react";
import { Suspense } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Link } from "react-router-dom";
import { useHistorikkV2 } from "@/core/api/queries/useHistorikkV2";
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
          FallbackComponent={() =>
            Feilmelding({
              message:
                "Noe gikk galt, og du får dessverre ikke sett alle deltakelser. Prøv igjen senere. ",
            })
          }
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
            Feilmelding({
              message:
                "Noe gikk galt, og du får dessverre ikke sett historikk. Prøv igjen senere. ",
            })
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
        <BodyShort className={styles.info}>
          <InformationSquareFillIcon color="#236B7D" fontSize={20} aria-hidden />
          Se Arena og “Tiltaksgjennomføring - avtaler” for å få den totale oversikten over brukerens
          deltakelse på arbeidsmarkedstiltak.
        </BodyShort>
      </VStack>
    </main>
  );
}

function Historikk() {
  const { data } = useHistorikkV2();
  if (!data) {
    return null;
  }

  const { historikk } = data;

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Historikk
      </Heading>
      {historikk.length > 0
        ? historikk.map((hist) => {
            return <HistorikkKort key={hist.deltakerId} historikk={hist} />;
          })
        : null}
    </VStack>
  );
}

function Utkast() {
  const { data } = useHistorikkV2();
  if (!data) {
    return null;
  }

  const { aktive } = data;

  return (
    <VStack gap="5">
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
