import { Alert } from "@navikt/ds-react";
import { useAvtalerForTiltakstype } from "../../../api/avtaler/useAvtalerForTiltakstype";
import { Avtalerad } from "../../../components/avtaler/Avtalerad";
import { Laster } from "../../../components/Laster";
import { ListeheaderAvtaler } from "../../../components/listeelementer/Listeheader";
import styles from "../../../components/listeelementer/Listeelementer.module.scss";

export function AvtaleTabell() {
  const { data, isLoading, error } = useAvtalerForTiltakstype();

  if (!data && isLoading) {
    return <Laster tekst="Henter avtaler for tiltakstype" />;
  }

  if (error) {
    return (
      <Alert variant="error">Klarte ikke hente avtaler for tiltakstype</Alert>
    );
  }

  const avtaler = data?.data ?? [];

  if (avtaler.length === 0) {
    return (
      <Alert variant="info">
        Det finnes ingen avtaler for denne tiltakstypen
      </Alert>
    );
  }

  return (
    <ul className={styles.oversikt}>
      <ListeheaderAvtaler />
      {avtaler.map((avtale) => {
        return <Avtalerad key={avtale.id} avtale={avtale} />;
      })}
    </ul>
  );
}
