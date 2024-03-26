import { Alert, Tabs } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "../../components/laster/Laster";
import skjemaStyles from "../../components/skjema/Skjema.module.scss";
import styles from "../DetaljerInfo.module.scss";
import { RedaksjoneltInnholdPreview } from "../../components/redaksjonelt-innhold/RedaksjoneltInnholdPreview";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { useAtom } from "jotai";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { InlineErrorBoundary } from "../../ErrorBoundary";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";

export function AvtaleInfo() {
  const { data: bruker } = useHentAnsatt();
  const { data: avtale, isPending, isError } = useAvtale();

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  if (!bruker || isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (isError) {
    return <Alert variant="error">Klarte ikke laste avtale</Alert>;
  }

  return (
    <div className={styles.info_container} data-testid="avtale_info-container">
      <Tabs defaultValue={activeTab}>
        <Tabs.List className={skjemaStyles.tabslist}>
          <div>
            <Tabs.Tab label="Detaljer" value="detaljer" onClick={() => setActiveTab("detaljer")} />
            <Tabs.Tab
              label="Redaksjonelt innhold"
              value="redaksjonelt-innhold"
              onClick={() => setActiveTab("redaksjonelt-innhold")}
            />
          </div>
          <AvtaleKnapperad bruker={bruker} avtale={avtale} />
        </Tabs.List>
        <Tabs.Panel value="detaljer">
          <InlineErrorBoundary>
            <AvtaleDetaljer />
          </InlineErrorBoundary>
        </Tabs.Panel>
        <Tabs.Panel value="redaksjonelt-innhold">
          <InlineErrorBoundary>
            <RedaksjoneltInnholdPreview
              tiltakstypeId={avtale.tiltakstype.id}
              beskrivelse={avtale.beskrivelse}
              faneinnhold={avtale.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
