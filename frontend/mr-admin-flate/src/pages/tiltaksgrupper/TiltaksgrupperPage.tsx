import { Button, Heading, Search } from "@navikt/ds-react";
import { Link } from "react-router-dom";
import React from "react";
import styles from "../Page.module.scss";
import { TiltaksgruppeOversikt } from "../../components/tiltaksgrupper/TiltaksgruppeOversikt";

export function TiltaksgrupperPage() {
  return (
    <>
      <div className={styles.header_wrapper}>
        <Heading size="large">Oversikt over tiltaksgrupper</Heading>
        <Link to="opprett" className={styles.opprettknappseksjon}>
          <Button variant="tertiary">Opprett ny tiltaksgruppe</Button>
        </Link>
      </div>
      <TiltaksgruppeOversikt />
    </>
  );
}
