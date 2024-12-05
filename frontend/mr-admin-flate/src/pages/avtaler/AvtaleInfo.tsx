import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Laster } from "@/components/laster/Laster";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { Toggles } from "@mr/api-client";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { Alert, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import styles from "./AvtaleInfo.module.scss";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { AvtalePrisOgFakturering } from "./AvtalePrisOgFakturering";
import { useAvtale } from "../../api/avtaler/useAvtale";

export function AvtaleInfo() {
  const { data: bruker } = useHentAnsatt();
  const { data: avtale, isPending, isError } = useAvtale();

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );

  if (!bruker || isPending) {
    return <Laster tekst="Laster avtale..." />;
  }

  if (isError) {
    return <Alert variant="error">Klarte ikke laste avtale</Alert>;
  }

  return (
    <InfoContainer dataTestId="avtale_info-container">
      <Tabs defaultValue={activeTab}>
        <Tabs.List className={styles.tabslist}>
          <div>
            <Tabs.Tab label="Detaljer" value="detaljer" onClick={() => setActiveTab("detaljer")} />
            {enableOpprettTilsagn && (
              <Tabs.Tab
                label="Pris og fakturering"
                value="pris-og-fakturering"
                onClick={() => setActiveTab("pris-og-fakturering")}
              />
            )}
            <Tabs.Tab
              label="Personvern"
              value="personvern"
              onClick={() => setActiveTab("personvern")}
            />
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
        {enableOpprettTilsagn && (
          <Tabs.Panel value="pris-og-fakturering">
            <InlineErrorBoundary>
              <AvtalePrisOgFakturering />
            </InlineErrorBoundary>
          </Tabs.Panel>
        )}
        <Tabs.Panel value="redaksjonelt-innhold">
          <InlineErrorBoundary>
            <RedaksjoneltInnholdPreview
              tiltakstype={avtale.tiltakstype}
              beskrivelse={avtale.beskrivelse}
              faneinnhold={avtale.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
        <Tabs.Panel value="personvern">
          <InlineErrorBoundary>
            <AvtalePersonvern />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </InfoContainer>
  );
}
