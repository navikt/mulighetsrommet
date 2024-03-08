import { PlusIcon } from "@navikt/aksel-icons";
import { Heading, VStack } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import styles from "./Landingsside.module.scss";
import { HistorikkKort } from "../historikk/HistorikkKort";
import { useHistorikkFraKomet } from "../../../core/api/queries/useHistorikkFraKomet";
import { UtkastKort } from "../historikk/UtkastKort";

export function Landingsside() {
  return (
    <main className="mulighetsrommet-veileder-flate">
      <VStack gap="10" className={styles.container}>
        <Utkast />
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
  const { data } = useHistorikkFraKomet();
  if (!data) {
    return null;
  }

  const { historikk } = data;

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Historikk
      </Heading>
      {historikk.map((hist) => {
        return <HistorikkKort key={hist.deltakerId} historikk={hist} />;
      })}
    </VStack>
  );
}

function Utkast() {
  const { data } = useHistorikkFraKomet();
  if (!data) {
    return null;
  }

  const { aktive } = data;

  return (
    <VStack gap="5">
      <Heading level="3" size="medium">
        Utkast
      </Heading>
      {aktive.map((utkast) => {
        return <UtkastKort key={utkast.deltakerId} utkast={utkast} />;
      })}
    </VStack>
  );
}
