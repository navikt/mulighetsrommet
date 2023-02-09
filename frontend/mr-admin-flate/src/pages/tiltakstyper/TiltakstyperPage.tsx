import { Heading } from "@navikt/ds-react";
import { FilterTiltakstyper } from "../../components/tiltakstyper/FilterTiltakstyper";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import { MainContent } from "../../components/visuals/MainContent";
import { NavigeringHeader } from "../forside/NavigeringHeader";
import styles from "../Page.module.scss";

export function TiltakstyperPage() {
  return (
    <>
      <NavigeringHeader />
      <MainContent>
        <div className={styles.header_wrapper}>
          <Heading level="2" size="large">
            Oversikt over tiltakstyper
          </Heading>
        </div>
        <div className={styles.filterseksjon}>
          <FilterTiltakstyper />
        </div>
        <TiltakstyperOversikt />
      </MainContent>
    </>
  );
}
