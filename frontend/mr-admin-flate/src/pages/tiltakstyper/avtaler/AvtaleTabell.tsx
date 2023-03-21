import { Alert } from "@navikt/ds-react";
import { useAvtaler } from "../../../api/avtaler/useAvtaler";
import { Avtalerad } from "../../../components/avtaler/Avtalerad";
import { Laster } from "../../../components/laster/Laster";
import styles from "../../../components/listeelementer/Listeelementer.module.scss";
import { ListeheaderAvtaler } from "../../../components/listeelementer/Listeheader";

export function AvtaleTabell() {
  const { data, isLoading, error } = useAvtaler();

  if (!data && isLoading) {
    return <Laster tekst="Henter avtaler" />;
  }

  if (error) {
    return <Alert variant="error">Klarte ikke hente avtaler</Alert>;
  }

  const avtaler = data?.data ?? [];

  if (avtaler.length === 0) {
    return <Alert variant="info">Fant ingen avtaler</Alert>;
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
