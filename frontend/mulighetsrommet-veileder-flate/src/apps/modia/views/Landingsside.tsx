import {
  ArrowForwardIcon,
  ExternalLinkIcon,
  HourglassBottomFilledIcon,
  LocationPinIcon,
  PlusIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  Link as AkselLink,
  Heading,
  HStack,
  Skeleton,
  Tabs,
  VStack,
  Box,
  HGrid,
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
import ingenFunnImg from "public/ingen-funn.svg";

function Feilmelding({ message }: { message: string }) {
  return (
    <Alert aria-live="polite" variant="error" style={{ marginTop: "1rem" }}>
      {message}
    </Alert>
  );
}

export function Landingsside() {
  const { logEvent } = useLogEvent();
  const [activeTab, setActiveTab] = useState<"aktive" | "historikk" | "delt-i-dialogen">("aktive");

  return (
    <main className="mulighetsrommet-veileder-flate">
      <HGrid
        columns={{
          xs: "1fr",
          lg: "25% minmax(500px, 1000px)",
        }}
        gap="4"
      >
        <VStack align={{ xs: "start", lg: "end" }} className={styles.cta_container}>
          <Link
            data-testid="finn-nytt-arbeidsmarkedstiltak-btn"
            className={styles.cta_link}
            to="/arbeidsmarkedstiltak/oversikt"
          >
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </VStack>
        <VStack gap="4">
          <FeedbackFraUrl />
          <Heading size="large">Oversikt over brukerens tiltak</Heading>
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
              <Tabs.Tab
                data-testid="aktive-tab"
                label={
                  <HStack gap="1">
                    <LocationPinIcon />
                    Aktive tiltak
                  </HStack>
                }
                value="aktive"
                onClick={() => setActiveTab("aktive")}
              />
              <Tabs.Tab
                data-testid="historikk-tab"
                label={
                  <HStack gap="1">
                    <HourglassBottomFilledIcon />
                    Tiltakshistorikk
                  </HStack>
                }
                value="historikk"
                onClick={() => setActiveTab("historikk")}
              />
              <Tabs.Tab
                data-testid="delt-i-dialogen-tab"
                label={
                  <HStack gap="1">
                    <ArrowForwardIcon />
                    Delt i dialogen
                  </HStack>
                }
                value="delt-i-dialogen"
                onClick={() => setActiveTab("delt-i-dialogen")}
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
                <Suspense
                  fallback={
                    <Skeleton
                      className={styles.skeleton}
                      variant="rounded"
                      height="10rem"
                      width="40rem"
                    />
                  }
                >
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
                    <VStack gap="5" className={styles.skeleton}>
                      <Skeleton variant="rounded" height="10rem" width="40rem" />
                      <Skeleton variant="rounded" height="10rem" width="40rem" />
                    </VStack>
                  }
                >
                  <Historikk />
                </Suspense>
              </ErrorBoundary>
            </Tabs.Panel>
            <Tabs.Panel value="delt-i-dialogen">
              <Container>
                <ErrorBoundary
                  FallbackComponent={() =>
                    Feilmelding({
                      message:
                        "Noe gikk galt, og du får dessverre ikke sett tiltak du har delt med bruker via dialogen. Prøv igjen senere. ",
                    })
                  }
                >
                  <Suspense
                    fallback={
                      <VStack gap="2">
                        <Skeleton height="3rem" />
                        <Skeleton height="3rem" />
                        <Skeleton height="3rem" />
                      </VStack>
                    }
                  >
                    <DelMedBrukerHistorikk />
                  </Suspense>
                </ErrorBoundary>
              </Container>
            </Tabs.Panel>
          </Tabs>
        </VStack>
      </HGrid>
    </main>
  );
}

function Container(props: { children: React.ReactNode }) {
  return (
    <VStack padding="2" gap="4" width={"100%"}>
      {props.children}
    </VStack>
  );
}

function Aktive() {
  const { data: aktive } = useTiltakshistorikkForBruker("AKTIVE");

  return (
    <Container>
      {aktive.map((utkast) => {
        return <DeltakelseKort key={utkast.id} deltakelse={utkast} />;
      })}
      {aktive.length === 0 && <IngenFunnetBox title="Brukeren har ingen aktive tiltak" />}
      <Alert variant="info">
        For oversikt over tiltakstypene “Sommerjobb”, “Midlertidig lønnstilskudd”, og “Varig
        lønnstilskudd” se <TeamTiltakLenke />
      </Alert>
    </Container>
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
  const { data: historiske } = useTiltakshistorikkForBruker("HISTORISKE");

  return (
    <Container>
      {historiske.map((hist) => {
        return <DeltakelseKort key={hist.id} deltakelse={hist} />;
      })}
      {historiske.length === 0 && <IngenFunnetBox title="Brukeren har ingen tidligere tiltak" />}
      <Alert variant="info">
        Vi viser bare historikk 5 år tilbake i tid. For oversikt over tiltakstypene “Sommerjobb”,
        “Midlertidig lønnstilskudd”, og “Varig lønnstilskudd” se <TeamTiltakLenke />
      </Alert>
    </Container>
  );
}

export function IngenFunnetBox(props: { title: string }) {
  return (
    <Box background="bg-default" borderRadius="medium" padding="5">
      <VStack align="center">
        <img
          src={ingenFunnImg}
          alt="Bilde av forstørrelsesglass som ser på et dokument"
          className={styles.tom_eske_img}
        />
        <Heading level="2" size="medium">
          {props.title}
        </Heading>
      </VStack>
    </Box>
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
    <Alert
      data-testid="feedback-fra-url"
      size="small"
      closeButton
      variant="success"
      onClose={onClose}
    >
      {successFeedbackHeading ? (
        <Heading size="small" spacing>
          {decodeURIComponent(successFeedbackHeading)}
        </Heading>
      ) : null}
      {decodeURIComponent(successFeedbackBody)}
    </Alert>
  );
}
