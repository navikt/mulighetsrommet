import { PlusIcon } from "@navikt/aksel-icons";
import { Heading, VStack } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import styles from "./Landingsside.module.scss";
import { HistorikkKort } from "../historikk/HistorikkKort";
import { useHistorikkFraKomet } from "../../../core/api/queries/useHistorikkFraKomet";

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <VStack gap="10" className={styles.container}>
        <div>
          <Link className={styles.cta_link} to="/arbeidsmarkedstiltak/oversikt">
            <PlusIcon color="white" fontSize={30} aria-hidden /> Finn nytt arbeidsmarkedstiltak
          </Link>
        </div>
        <Historikk />
      </VStack>
    </main>
  );
}

function Historikk() {
  const { data: historikk } = useHistorikkFraKomet();

  if (!historikk) {
    return null;
  }

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Historikk
      </Heading>
      {historikk.map((hist) => {
        return <HistorikkKort key={hist.id} historikk={hist} />;
      })}
    </VStack>
  );
}
