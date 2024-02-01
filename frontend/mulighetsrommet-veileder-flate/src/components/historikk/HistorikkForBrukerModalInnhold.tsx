import { ExternalLinkIcon } from "@navikt/aksel-icons";
import { Alert, BodyShort, Detail, Link, Loader } from "@navikt/ds-react";
import { HistorikkForBruker as IHistorikkForBruker } from "mulighetsrommet-api-client";
import { PORTEN } from "mulighetsrommet-frontend-common/constants";
import { useHentHistorikk } from "../../core/api/queries/useHentHistorikk";
import { formaterDato } from "../../utils/Utils";
import styles from "./HistorikkForBrukerModal.module.scss";
import { StatusBadge } from "./Statusbadge";

export function HistorikkForBrukerModalInnhold() {
  const { data, isLoading, isError } = useHentHistorikk();
  if (isLoading && !data) return <Loader />;

  if (isError) return <Alert variant="error">Kunne ikke hente brukerens tiltakshistorikk</Alert>;

  const sorterPaaFraDato = (a: IHistorikkForBruker, b: IHistorikkForBruker) => {
    if (!a.fraDato || !b.fraDato) return -1; // Flytt deltakelser uten fraDato bakerst

    return new Date(a.fraDato ?? "").getTime() - new Date(b.fraDato ?? "").getTime();
  };

  const venter =
    data?.filter((deltak) => ["VENTER"].includes(deltak.status ?? "")).sort(sorterPaaFraDato) ?? [];
  const deltar =
    data?.filter((deltak) => ["DELTAR"].includes(deltak.status ?? "")).sort(sorterPaaFraDato) ?? [];
  const avsluttet =
    data?.filter((deltak) => ["AVSLUTTET"].includes(deltak.status ?? "")).sort(sorterPaaFraDato) ??
    [];
  const ikkeAktuell =
    data
      ?.filter((deltak) => ["IKKE_AKTUELL"].includes(deltak.status ?? ""))
      .sort(sorterPaaFraDato) ?? [];

  const tiltak = [...venter, ...deltar, ...avsluttet, ...ikkeAktuell];

  return (
    <>
      <Alert variant="info" style={{ marginBottom: "1rem" }}>
        Vi viser bare tiltak 5 år tilbake i tid. Vær oppmerksom på at tiltak som er flyttet ut fra
        Arena kan mangle i historikken.
      </Alert>
      <ul className={styles.historikk_for_bruker_liste}>
        {tiltak?.map((historikk) => {
          return (
            <li key={historikk.id} className={styles.historikk_for_bruker_listeelement}>
              <div className={styles.historikk_for_bruker_data}>
                <BodyShort size="small">{historikk.tiltaksnavn}</BodyShort>

                <div className={styles.historikk_for_bruker_arrangor_tiltakstype}>
                  <Detail className={styles.historikk_for_bruker_tiltakstype}>
                    {historikk.tiltakstype}
                  </Detail>
                  <Detail>•</Detail>
                  <Detail className={styles.historikk_for_bruker_arrangor}>
                    {historikk.arrangor?.navn}
                  </Detail>
                </div>
              </div>
              <div className={styles.historikk_for_bruker_status_dato}>
                <StatusBadge status={historikk.status} />
                <div className={styles.historikk_datoer}>
                  <Detail>{formaterDato(historikk.fraDato ?? "")}</Detail> -{" "}
                  <Detail>{formaterDato(historikk.tilDato ?? "")}</Detail>
                </div>
              </div>
            </li>
          );
        })}
      </ul>
      <ViVilHoreFraDeg />
    </>
  );
}

function ViVilHoreFraDeg() {
  return (
    <>
      <h4>Vi vil høre fra deg</h4>
      <BodyShort>
        Vi jobber med utvikling av historikk-funksjonaliteten og vi ønsker å høre fra deg som har
        tanker om hvordan historikken burde presenteres og fungere.{" "}
        <Link href={PORTEN}>
          Send oss gjerne en melding via Porten <ExternalLinkIcon />
        </Link>
      </BodyShort>
    </>
  );
}
