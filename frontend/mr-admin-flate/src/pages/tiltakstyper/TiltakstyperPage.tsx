import { Heading, Button, Search } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { TiltakstyperOversikt } from "../../components/tiltakstyper/TiltakstyperOversikt";
import styles from "../tiltaksgjennomforinger/Oversikt.module.scss";
import React from "react";
import { useAtom } from "jotai";
import { tiltakstypefilter } from "../../api/atoms";

export function TiltakstyperPage() {
  const { data: toggles } = useFeatureToggles();
  const [sokefilter, setSokefilter] = useAtom(tiltakstypefilter);

  return (
    <>
      <Link to="/">Hjem</Link>
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltakstyper
      </Heading>
      <div className={styles.opprettknappseksjon}>
        {toggles?.["mulighetsrommet.enable-opprett-tiltakstype"] ? (
          <Link style={{ textDecoration: "none" }} to="opprett">
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
          className={styles.sokefelt}
          aria-label="Søk etter tiltakstype"
          data-testid="filter_sokefelt"
          size="small"
        />
      </div>
      <TiltakstyperOversikt />
    </>
  );
}
