import { useHistorikkV2 } from "@/api/queries/useHistorikkV2";
import { InformationSquareFillIcon, PlusIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Heading, Skeleton, VStack } from "@navikt/ds-react";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Link, useLocation } from "react-router-dom";
import { DeltakelseKort } from "../historikk/DeltakelseKort";
import styles from "./Landingsside.module.scss";

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
      <VStack gap="5" className={styles.container}>
        <ErrorBoundary
          FallbackComponent={() =>
            Feilmelding({
              message:
                "Noe gikk galt, og du får dessverre ikke sett alle deltakelser. Prøv igjen senere. ",
            })
          }
        >
          <Suspense fallback={<Skeleton variant="rounded" height="10rem" width="40rem" />}>
            <FeedbackFraUrl />
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
              message: "Noe gikk galt, og du får dessverre ikke sett historikk. Prøv igjen senere.",
            })
          }
        >
          <Suspense
            fallback={
              <VStack gap="5">
                <Skeleton variant="rounded" height="10rem" width="40rem" />
                <Skeleton variant="rounded" height="10rem" width="40rem" />
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
      {historikk.map((hist) => {
        return (
          <>
            <DeltakelseKort key={hist.deltakerId} deltakelse={hist} />
          </>
        );
      })}
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
      {aktive.map((utkast) => {
        return (
          <>
            <DeltakelseKort key={utkast.deltakerId} deltakelse={utkast} />
          </>
        );
      })}
    </VStack>
  );
}

function FeedbackFraUrl() {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const successFeedbackHeading = queryParams.get("success_feedback_heading");
  const successFeedbackBody = queryParams.get("success_feedback_body");
  const [show, setShow] = useState(true);

  function onClose() {
    const searchParams = new URLSearchParams(location.search);
    searchParams.delete("success_feedback_heading");
    searchParams.delete("success_feedback_body");
    window.history.replaceState(null, "", `${location.pathname}?${searchParams}`);
    setShow(false);
  }

  if (!successFeedbackBody) {
    return null;
  }

  if (!show) {
    return null;
  }

  return (
    <Alert size="small" closeButton variant="success" onClose={onClose}>
      {successFeedbackHeading ? (
        <Heading size="small" spacing>
          {decodeURIComponent(successFeedbackHeading)}
        </Heading>
      ) : null}
      {decodeURIComponent(successFeedbackBody)}
    </Alert>
  );
}
