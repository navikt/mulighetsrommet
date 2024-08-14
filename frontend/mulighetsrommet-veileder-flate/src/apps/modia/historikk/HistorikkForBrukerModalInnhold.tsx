import { PortenLink } from "@/components/PortenLink";
import { formaterDato } from "@/utils/Utils";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Detail, HStack, Heading, Loader, VStack } from "@navikt/ds-react";
import styles from "./HistorikkForBrukerModal.module.scss";
import { StatusBadge } from "./Statusbadge";
import { useTiltakshistorikkForBruker } from "@/api/queries/useTiltakshistorikkForBruker";
import { AmtDeltakerStatusAarsak, TiltakshistorikkAdminDto } from "@mr/api-client";

export function HistorikkForBrukerModalInnhold() {
  const { data: historikk, isPending, isError } = useTiltakshistorikkForBruker();

  if (isPending) return <Loader />;

  if (isError || !historikk)
    return <Alert variant="error">Kunne ikke hente brukerens tiltakshistorikk</Alert>;

  const sorterPaaFraDato = (a: TiltakshistorikkAdminDto, b: TiltakshistorikkAdminDto) => {
    if (!a.startDato) return 1;
    if (!b.startDato) return -1;

    return new Date(b.startDato ?? "").getTime() - new Date(a.startDato ?? "").getTime();
  };

  return (
    <div style={{ marginTop: "1rem" }}>
      {historikk.length === 0 ? (
        <Alert variant="info" style={{ marginBottom: "1rem" }}>
          Vi finner ingen registrerte tiltak på brukeren
        </Alert>
      ) : null}
      <Alert variant="info" style={{ marginBottom: "1rem" }}>
        Vi viser bare tiltak 5 år tilbake i tid. Vær oppmerksom på at tiltak som er flyttet ut fra
        Arena kan mangle i historikken.
      </Alert>
      <ul className={styles.historikk_for_bruker_liste}>
        {historikk.sort(sorterPaaFraDato).map((historikk) => {
          return (
            <li key={historikk.id} className={styles.historikk_for_bruker_listeelement}>
              <VStack>
                <HStack gap="10">{<small>{historikk.tiltakstypeNavn.toUpperCase()}</small>}</HStack>
                <Heading size="small" level="4">
                  {historikk.tiltakNavn}
                </Heading>
                <HStack align={"end"} gap="5">
                  <StatusBadge status={historikk.status} />
                  {historikk.opphav === "TEAM_KOMET" && historikk.status.aarsak && (
                    <Detail>{`Årsak: ${amtAarsakToString(historikk.status.aarsak)}`}</Detail>
                  )}
                  {historikk.startDato ? (
                    <BodyShort size="small">
                      {historikk.startDato && !historikk.sluttDato
                        ? `Oppstartsdato ${formaterDato(historikk.startDato)}`
                        : [historikk.startDato, historikk.sluttDato]
                            .filter(Boolean)
                            .map((dato) => dato && formaterDato(dato))
                            .join(" - ")}
                    </BodyShort>
                  ) : null}
                </HStack>
              </VStack>
            </li>
          );
        })}
      </ul>
      <ViVilHoreFraDeg />
    </div>
  );
}

function ViVilHoreFraDeg() {
  return (
    <>
      <h4>Vi vil høre fra deg</h4>
      <BodyShort>
        Vi jobber med utvikling av historikk-funksjonaliteten og vi ønsker å høre fra deg som har
        tanker om hvordan historikken burde presenteres og fungere.{" "}
        <PortenLink>
          Send oss gjerne en melding via Porten <ExternalLinkIcon />
        </PortenLink>
      </BodyShort>
    </>
  );
}

function amtAarsakToString(aarsak: AmtDeltakerStatusAarsak): string {
  switch (aarsak) {
    case AmtDeltakerStatusAarsak.SYK:
      return "Syk";
    case AmtDeltakerStatusAarsak.FATT_JOBB:
      return "Fått jobb";
    case AmtDeltakerStatusAarsak.TRENGER_ANNEN_STOTTE:
      return "Trenger anne støtte";
    case AmtDeltakerStatusAarsak.FIKK_IKKE_PLASS:
      return "Fikk ikke plass";
    case AmtDeltakerStatusAarsak.UTDANNING:
      return "Utdanning";
    case AmtDeltakerStatusAarsak.FERDIG:
      return "Ferdig";
    case AmtDeltakerStatusAarsak.AVLYST_KONTRAKT:
      return "Avlyst kontrakt";
    case AmtDeltakerStatusAarsak.IKKE_MOTT:
      return "Ikke møtt";
    case AmtDeltakerStatusAarsak.FEILREGISTRERT:
      return "Feilregistrert";
    case AmtDeltakerStatusAarsak.OPPFYLLER_IKKE_KRAVENE:
      return "Oppfyller ikke kravene";
    case AmtDeltakerStatusAarsak.ANNET:
      return "Annet";
    case AmtDeltakerStatusAarsak.SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT:
      return "Samarbeidet med arrangøren er avbrutt";
  }
}
