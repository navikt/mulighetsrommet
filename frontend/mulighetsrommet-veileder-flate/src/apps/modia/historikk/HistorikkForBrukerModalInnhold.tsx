import { useTiltakshistorikkForBruker } from "@/api/queries/useTiltakshistorikkForBruker";
import { PortenLink } from "@/components/PortenLink";
import { formaterDato } from "@/utils/Utils";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Detail, HStack, Heading, Loader, VStack } from "@navikt/ds-react";
import styles from "./HistorikkForBrukerModal.module.scss";
import { StatusBadge } from "./Statusbadge";

export function HistorikkForBrukerModalInnhold() {
  const { data: historikk, isPending, isError } = useTiltakshistorikkForBruker();

  if (isPending) return <Loader />;

  if (isError || !historikk)
    return <Alert variant="error">Kunne ikke hente brukerens tiltakshistorikk</Alert>;

  const { historiske = [] } = historikk;

  return (
    <div style={{ marginTop: "1rem" }}>
      {historiske.length === 0 ? (
        <Alert variant="info" style={{ marginBottom: "1rem" }}>
          Vi finner ingen registrerte tiltak på brukeren
        </Alert>
      ) : null}
      <Alert variant="info" style={{ marginBottom: "1rem" }}>
        Vi viser bare tiltak 5 år tilbake i tid. Vær oppmerksom på at tiltak som er flyttet ut fra
        Arena kan mangle i historikken.
      </Alert>
      <ul className={styles.historikk_for_bruker_liste}>
        {historiske.map((historikk) => {
          return (
            <li key={historikk.id} className={styles.historikk_for_bruker_listeelement}>
              <VStack>
                <HStack gap="10">{<small>{historikk.tiltakstypeNavn.toUpperCase()}</small>}</HStack>
                <Heading size="small" level="4">
                  {historikk.tittel}
                </Heading>
                <HStack align={"end"} gap="5">
                  <StatusBadge status={historikk.status} />
                  {historikk.status.aarsak && (
                    <Detail>{`Årsak: ${historikk.status.aarsak}`}</Detail>
                  )}
                  {historikk.periode ? (
                    <BodyShort size="small">
                      {historikk.periode.startdato && !historikk.periode.sluttdato
                        ? `Oppstartsdato ${formaterDato(historikk.periode.startdato)}`
                        : [historikk.periode.startdato, historikk.periode.sluttdato]
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
