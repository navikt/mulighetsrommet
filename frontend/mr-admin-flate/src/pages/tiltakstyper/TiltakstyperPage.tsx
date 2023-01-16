import { Button, Heading, Search } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import React from "react";
import { useAtom } from "jotai";
import { tiltakstypefilter } from "../../api/atoms";
import styles from "./TiltakstyperPage.module.scss";

export function TiltakstyperPage() {
  const { data: toggles } = useFeatureToggles();
  const [sokefilter, setSokefilter] = useAtom(tiltakstypefilter);

  return (
    <>
      <div className={styles.header_wrapper}>
        <Heading size="large">Oversikt over tiltakstyper</Heading>
        {toggles?.["mulighetsrommet.enable-opprett-tiltakstype"] ? (
          <Link to="opprett" className={styles.opprettknappseksjon}>
            <Button variant="tertiary">Opprett ny tiltakstype</Button>
          </Link>
        ) : null}
      </div>
      <div className={styles.filterseksjon}>
        <Search
          label=""
          placeholder=""
          hideLabel
          variant="simple"
          onChange={(e: string) => setSokefilter(e)}
          value={sokefilter}
          aria-label="Søk etter tiltakstype"
          data-testid="filter_sokefelt"
          size="small"
        />
      </div>
      <TiltakstyperOversikt />
    </>
  );
}
