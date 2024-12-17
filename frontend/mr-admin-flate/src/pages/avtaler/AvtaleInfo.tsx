import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InfoContainer } from "@/components/skjema/InfoContainer";
import { Toggles } from "@mr/api-client";
import { InlineErrorBoundary } from "@mr/frontend-common";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useLoaderData } from "react-router-dom";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import styles from "./AvtaleInfo.module.scss";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { avtaleLoader } from "./avtaleLoader";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { AvtalePrisOgFakturering } from "./AvtalePrisOgFakturering";

export function AvtaleInfo() {
  const { avtale, ansatt } = useLoaderData<typeof avtaleLoader>();

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );

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
          <AvtaleKnapperad ansatt={ansatt} avtale={avtale} />
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
