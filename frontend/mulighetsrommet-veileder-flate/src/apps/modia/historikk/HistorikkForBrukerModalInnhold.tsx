import { useTiltakshistorikkForBruker } from "@/api/queries/useTiltakshistorikkForBruker";
import { PortenLink } from "@/components/PortenLink";
import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Loader } from "@navikt/ds-react";
import { DeltakelseKort } from "./DeltakelseKort";
import styles from "./HistorikkForBrukerModal.module.scss";

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
              <DeltakelseKort size="small" deltakelse={historikk} />
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
