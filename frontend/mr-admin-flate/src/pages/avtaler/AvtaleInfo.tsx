import { Alert, Tabs } from "@navikt/ds-react";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "@/components/laster/Laster";
import styles from "./AvtaleInfo.module.scss";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { useAtom } from "jotai";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client";
import { AvtalePrisOgFakturering } from "./AvtalePrisOgFakturering";

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
