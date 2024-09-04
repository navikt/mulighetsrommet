import { ExternalLinkIcon, PlusIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Link as AkselLink,
  Heading,
  HStack,
  Skeleton,
  Tabs,
  VStack,
} from "@navikt/ds-react";
import { Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Link, useLocation } from "react-router-dom";
import { useTiltakshistorikkForBruker } from "../../../api/queries/useTiltakshistorikkForBruker";
import { DeltakelseKort } from "../historikk/DeltakelseKort";
import styles from "./Landingsside.module.scss";
import { DelMedBrukerHistorikk } from "../delMedBruker/DelMedBrukerHistorikk";
import { isProduction } from "@/environment";
import { useLogEvent } from "@/logging/amplitude";

function Feilmelding({ message }: { message: string }) {
  return (
    <Alert aria-live="polite" variant="error">
      {message}
    </Alert>
  );
}

export function Landingsside() {
  const { logEvent } = useLogEvent();
  const [activeTab, setActiveTab] = useState<"aktive" | "historikk">("aktive");

  return (
    <main className="mulighetsrommet-veileder-flate">
      <VStack gap="4" padding="4">
        <FeedbackFraUrl />
        <HStack justify="space-between">
          <Heading size="large">Oversikt over brukerens tiltak</Heading>
          <Link
            data-testid="finn-nytt-arbeidsmarkedstiltak-btn"
            className={styles.cta_link}
            to="/arbeidsmarkedstiltak/oversikt"
          >
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </HStack>
        <Tabs
          onChange={(fane) => {
            logEvent({
              name: "arbeidsmarkedstiltak.landingsside.fane-valgt",
              data: {
                action: fane,
              },
            });
          }}
          defaultValue={activeTab}
        >
          <Tabs.List className={styles.tabslist}>
            <Tabs.Tab label="Aktive" value="aktive" onClick={() => setActiveTab("aktive")} />
            <Tabs.Tab
              label="Historikk"
              value="historikk"
              onClick={() => setActiveTab("historikk")}
            />
            <Tabs.Tab
              label="Delt med bruker"
              value="delt-med-bruker"
              onClick={() => setActiveTab("historikk")}
            />
          </Tabs.List>
          <Tabs.Panel value="aktive">
            <ErrorBoundary
              FallbackComponent={() =>
                Feilmelding({
                  message:
                    "Noe gikk galt, og du får dessverre ikke sett alle deltakelser. Prøv igjen senere. ",
                })
              }
            >
              <Suspense fallback={<Skeleton variant="rounded" height="10rem" width="40rem" />}>
                <Aktive />
              </Suspense>
            </ErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="historikk">
            <ErrorBoundary
              FallbackComponent={() =>
                Feilmelding({
                  message:
                    "Noe gikk galt, og du får dessverre ikke sett historikk. Prøv igjen senere.",
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
          </Tabs.Panel>
          <Tabs.Panel value="delt-med-bruker">
            <DelMedBrukerHistorikk />
          </Tabs.Panel>
        </Tabs>
      </VStack>
    </main>
  );
}

function Aktive() {
  const { data } = useTiltakshistorikkForBruker();

  if (!data) {
    return null;
  }

  const { aktive } = data;

  return (
    <VStack padding="2" gap="4">
      {aktive.map((utkast) => {
        return <DeltakelseKort key={utkast.id} deltakelse={utkast} />;
      })}
      {aktive.length === 0 ? (
        <Alert variant="info">
          Vi finner ingen registrerte tiltak på brukeren. For oversikt over tilskudd til sommerjobb,
          midlertig, og -varig lønnstilskudd se “Tiltaksgjennomføring - avtaler”.
        </Alert>
      ) : (
        <Alert variant="info">
          For oversikt over tiltakstypene “Sommerjobb”, “Midlertidig lønnstilskudd”, og “Varig
          lønnstilskudd” se <TeamTiltakLenke />
        </Alert>
      )}
    </VStack>
  );
}

function TeamTiltakLenke() {
  const baseUrl = isProduction
    ? "https://tiltaksgjennomforing.intern.nav.no"
    : "https://tiltaksgjennomforing.intern.dev.nav.no";

  return (
    <AkselLink target="_blank" rel="noreferrer noopener" href={`${baseUrl}/tiltaksgjennomforing`}>
      Tiltaksgjennomføring - avtaler <ExternalLinkIcon title="Ikon for å åpne lenke i ny fane" />
    </AkselLink>
  );
}

function Historikk() {
  const { data } = useTiltakshistorikkForBruker();

  if (!data) {
    return null;
  }

  const { historiske } = data;

  return (
    <VStack padding="2" gap="4">
      {historiske.map((hist) => {
        return <DeltakelseKort key={hist.id} deltakelse={hist} />;
      })}
      {historiske.length === 0 ? (
        <Alert variant="info">
          Vi finner ingen registrerte tiltak på brukeren. Vi viser bare historikk 5 år tilbake i
          tid. For oversikt over tiltakstypene “Sommerjobb”, “Midlertidig lønnstilskudd”, og “Varig
          lønnstilskudd” se <TeamTiltakLenke />
        </Alert>
      ) : (
        <Alert variant="info">
          Vi viser bare historikk 5 år tilbake i tid. For oversikt over tiltakstypene “Sommerjobb”,
          “Midlertidig lønnstilskudd”, og “Varig lønnstilskudd” se <TeamTiltakLenke />
        </Alert>
      )}
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
