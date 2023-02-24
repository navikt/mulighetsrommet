import { Heading } from "@navikt/ds-react";
import { Tiltakstypefilter } from "../../components/filter/Tiltakstypefilter";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import { ListLayout } from "../../layouts/ListLayout";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";

export function TiltakstyperPage() {
  return (
    <main>
      <NavigeringHeader />
      <ListLayout>
        <div className={styles.header_wrapper}>
          <Heading level="2" size="large">
            Oversikt over tiltakstyper
          </Heading>
        </div>
        <Tiltakstypefilter />
        <TiltakstyperOversikt />
      </ListLayout>
    </main>
  );
}
