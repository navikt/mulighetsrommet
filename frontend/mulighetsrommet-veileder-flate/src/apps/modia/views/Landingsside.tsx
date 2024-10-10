import {
  ArrowForwardIcon,
  ExternalLinkIcon,
  HourglassBottomFilledIcon,
  LocationPinIcon,
  PlusIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  Box,
  Heading,
  HelpText,
  HGrid,
  HStack,
  Link as AkselLink,
  Skeleton,
  Tabs,
  VStack,
} from "@navikt/ds-react";
import { ReactNode, Suspense, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { Link, useLocation } from "react-router-dom";
import { useTiltakshistorikkForBruker } from "@/api/queries/useTiltakshistorikkForBruker";
import { DeltakelseKort } from "../historikk/DeltakelseKort";
import styles from "./Landingsside.module.scss";
import { DelMedBrukerHistorikk } from "../delMedBruker/DelMedBrukerHistorikk";
import { useLogEvent } from "@/logging/amplitude";
import ingenFunnImg from "public/ingen-funn.svg";
import { DeltakelserMelding } from "@mr/api-client";
import { TEAM_TILTAK_TILTAKSGJENNOMFORING_AVTALER_URL } from "@/constants";

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
                    <LocationPinIcon aria-hidden />
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
                    <HourglassBottomFilledIcon aria-hidden />
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
                    <ArrowForwardIcon aria-hidden />
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
                  <DeltakelserAktive />
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
                  <DeltakelserHistoriske />
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

function Container(props: { children: ReactNode }) {
  return (
    <VStack padding="2" gap="4" width={"100%"}>
      {props.children}
    </VStack>
  );
}

function DeltakelserAktive() {
  const { data } = useTiltakshistorikkForBruker("AKTIVE");

  return (
    <Container>
      {data.meldinger.includes(DeltakelserMelding.MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET) && (
        <ManglerSisteDeltakelserFraTeamKometMelding />
      )}
      {data.meldinger.includes(DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK) && (
        <ManglerDeltakelserFraTeamTiltakMelding />
      )}
      {data.deltakelser.map((deltakelse) => {
        return <DeltakelseKort key={deltakelse.id} deltakelse={deltakelse} />;
      })}
      {data.deltakelser.length === 0 && <IngenFunnetBox title="Brukeren har ingen aktive tiltak" />}
    </Container>
  );
}

function DeltakelserHistoriske() {
  const { data } = useTiltakshistorikkForBruker("HISTORISKE");

  return (
    <Container>
      {data.meldinger.includes(DeltakelserMelding.MANGLER_SISTE_DELTAKELSER_FRA_TEAM_KOMET) && (
        <ManglerSisteDeltakelserFraTeamKometMelding />
      )}
      {data.meldinger.includes(DeltakelserMelding.MANGLER_DELTAKELSER_FRA_TEAM_TILTAK) && (
        <ManglerDeltakelserFraTeamTiltakMelding />
      )}
      {data.deltakelser.map((deltakelse) => {
        return <DeltakelseKort key={deltakelse.id} deltakelse={deltakelse} />;
      })}
      {data.deltakelser.length === 0 && (
        <IngenFunnetBox title="Brukeren har ingen tidligere tiltak" />
      )}
      {data.meldinger.includes(DeltakelserMelding.HENTER_IKKE_DELTAKELSER_FRA_TEAM_TILTAK) && (
        <HenterIkkeDeltakelserFraTeamTiltakMeling />
      )}
      <Alert variant="info">Vi viser bare historikk 5 år tilbake i tid.</Alert>
    </Container>
  );
}

function ManglerSisteDeltakelserFraTeamKometMelding() {
  return (
    <Alert variant="warning">
      <HStack align="center">
        Vi får ikke kontakt med baksystemene og informasjon om deltakelser på gruppetiltakene kan
        derfor være utdatert.
        <HelpText>
          Gjelder følgende tiltakstyper:
          <ul>
            <li>Arbeidsforberedende trening</li>
            <li>Arbeidsmarkedsopplæring (gruppe)</li>
            <li>Arbeidsrettet rehabilitering</li>
            <li>Avklaring</li>
            <li>Digitalt oppfølgingstiltak</li>
            <li>Fag- og yrkesopplæring (gruppe)</li>
            <li>Jobbklubb</li>
            <li>Oppfølging</li>
            <li>Varig tilrettelagt arbeid i skjermet virksomhet</li>
          </ul>
        </HelpText>
      </HStack>
    </Alert>
  );
}

function ManglerDeltakelserFraTeamTiltakMelding() {
  return (
    <Alert variant="warning">
      <HStack align="center">
        Vi får ikke kontakt med baksystemene og informasjon om tiltak hos arbeidsgiver
        <TeamTiltakTiltaksgjennomforingAvtalerLink />
        mangler derfor i visningen.
        <HelpText>
          Gjelder følgende tiltakstyper:
          <ul>
            <li>Arbeidstrening</li>
            <li>Inkluderingstilskudd</li>
            <li>Mentor</li>
            <li>Midlertidig lønnstilskudd</li>
            <li>Tilskudd til sommerjobb</li>
            <li>Varig lønnstilskudd</li>
            <li>Varig tilrettelagt arbeid i ordinær virksomhet</li>
          </ul>
        </HelpText>
      </HStack>
    </Alert>
  );
}

function HenterIkkeDeltakelserFraTeamTiltakMeling() {
  return (
    <Alert variant="info">
      For oversikt over tiltakstypene “Sommerjobb”, “Midlertidig lønnstilskudd”, og “Varig
      lønnstilskudd” se <TeamTiltakTiltaksgjennomforingAvtalerLink />
    </Alert>
  );
}

function TeamTiltakTiltaksgjennomforingAvtalerLink() {
  return (
    <AkselLink
      target="_blank"
      rel="noreferrer noopener"
      href={`${TEAM_TILTAK_TILTAKSGJENNOMFORING_AVTALER_URL}/tiltaksgjennomforing?part=VEILEDER`}
    >
      Tiltaksgjennomføring - avtaler <ExternalLinkIcon title="Ikon for å åpne lenke i ny fane" />
    </AkselLink>
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
