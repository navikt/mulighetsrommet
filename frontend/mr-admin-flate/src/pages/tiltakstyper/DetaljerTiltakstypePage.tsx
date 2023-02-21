import { Alert, Heading, Tabs } from "@navikt/ds-react";
import classNames from "classnames";
import { useAtom } from "jotai";
import { Link } from "react-router-dom";
import { avtaleTabAtom, AvtaleTabs } from "../../api/atoms";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import { useTiltakstypeById } from "../../api/tiltakstyper/useTiltakstypeById";
import { Laster } from "../../components/Laster";
import { Tilbakelenke } from "../../components/navigering/Tilbakelenke";
import { AvtalerForTiltakstype } from "./avtaler/AvtalerForTiltakstype";
import styles from "./DetaljerTiltakstypePage.module.scss";
import { TiltakstypeDetaljer } from "./Tiltakstypedetaljer";

export function DetaljerTiltakstypePage() {
  const optionalTiltakstype = useTiltakstypeById();
  const [tabValgt, setTabValgt] = useAtom(avtaleTabAtom);
  const features = useFeatureToggles();

  if (!optionalTiltakstype.data && optionalTiltakstype.isFetching) {
    return <Laster tekst="Laster tiltakstype" />;
  }

  if (!optionalTiltakstype.data) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <div>
          <Link to="/">Til forside</Link>
        </div>
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
      <Tabs
        value={tabValgt}
        onChange={(value) => setTabValgt(value as AvtaleTabs)}
      >
        <Tabs.List className={classNames(styles.padding_detaljer)}>
          <Tabs.Tab value="arenaInfo" label="Arenainfo" />
          {features?.data &&
          features?.data["mulighetsrommet.vis-avtaler-for-tiltakstyper"] ? (
            <Tabs.Tab value="avtaler" label="Avtaler" />
          ) : null}
        </Tabs.List>
        <Tabs.Panel value="arenaInfo" className="h-24 w-full bg-gray-50 p-4">
          <div className={styles.padding_detaljer}>
            <TiltakstypeDetaljer />
          </div>
        </Tabs.Panel>
        <Tabs.Panel value="avtaler" className="h-24 w-full bg-gray-50 p-4">
          <div className={styles.padding_detaljer}>
            <AvtalerForTiltakstype />
          </div>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
