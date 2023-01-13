import { BodyShort, Heading, Search } from "@navikt/ds-react";
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
      <Heading className={styles.overskrift} size="large">
        Oversikt over tiltakstyper
      </Heading>
      {toggles?.["mulighetsrommet.enable-opprett-tiltakstype"] ? (
        <Link to="/tiltakstyper/opprett">Opprett tiltakstype</Link>
      ) : null}

      <BodyShort className={styles.body} size="small">
        Her finner du dine aktive tiltakstyper.
      </BodyShort>
      <Search
        label=""
        placeholder="Søk etter tiltakstype"
        hideLabel
        variant="simple"
        onChange={(e: string) => setSokefilter(e)}
        value={sokefilter}
        className={styles.sokefelt}
        aria-label="Søk etter tiltakstype"
        data-testid="filter_sokefelt"
        size="small"
      />
      <TiltakstyperOversikt />
    </>
  );
}
