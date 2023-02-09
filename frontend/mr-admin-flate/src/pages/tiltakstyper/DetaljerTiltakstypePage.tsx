import { Alert, Heading, Tabs } from "@navikt/ds-react";
import classNames from "classnames";
import { useState } from "react";
import { Link } from "react-router-dom";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";
import styles from "./DetaljerTiltakstypePage.module.scss";
import { TiltakstypeDetaljer } from "./Tiltakstypedetaljer";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const [tabValgt, setTabValgt] = useState("arenaInfo");

  if (optionalTiltakstype.isFetching) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <Link to="/">Til forside</Link>
      </Alert>
    );
  }

  const tiltakstype = optionalTiltakstype.data;
  return (
    <main>
      <div className={classNames(styles.header, styles.padding_detaljer)}>
        <Tilbakelenke>Tilbake</Tilbakelenke>
        <Heading size="large" level="2">
          {tiltakstype.navn}
        </Heading>
      </div>
      <Tabs value={tabValgt} onChange={setTabValgt}>
        <Tabs.List className={classNames(styles.padding_detaljer)}>
          <Tabs.Tab value="arenaInfo" label="Arenainfo" />
        </Tabs.List>
        <Tabs.Panel value="arenaInfo" className="h-24 w-full bg-gray-50 p-4">
          <div className={styles.padding_detaljer}>
            <TiltakstypeDetaljer />
          </div>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
