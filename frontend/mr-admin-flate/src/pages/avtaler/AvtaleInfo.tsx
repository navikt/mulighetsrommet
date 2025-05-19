import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { AvtalePrisOgFaktureringDetaljer } from "@/pages/avtaler/AvtalePrisOgFaktureringDetaljer";
import { Toggles } from "@mr/api-client-v2";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtaleKnapperad } from "./AvtaleKnapperad";
import { AvtalePersonvern } from "./AvtalePersonvern";

export function AvtaleInfo() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: ansatt } = useHentAnsatt();
  const { data: avtale } = useAvtale(avtaleId);

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  const { data: enableTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    avtale?.tiltakstype.tiltakskode ? [avtale.tiltakstype.tiltakskode] : [],
  );

  return (
    <div data-testid="avtale_info-container">
      <WhitePaddedBox data-testid="avtale_info-container">
        <Tabs defaultValue={activeTab}>
          <Tabs.List className="flex flex-row justify-between">
            <div>
              <Tabs.Tab
                label="Detaljer"
                value="detaljer"
                onClick={() => setActiveTab("detaljer")}
              />
              {enableTilsagn && (
                <Tabs.Tab label="Økonomi" value="okonomi" onClick={() => setActiveTab("okonomi")} />
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
              <AvtaleDetaljer avtale={avtale} />
            </InlineErrorBoundary>
          </Tabs.Panel>
          {enableTilsagn && (
            <Tabs.Panel value="okonomi">
              <InlineErrorBoundary>
                <AvtalePrisOgFaktureringDetaljer avtale={avtale} />
              </InlineErrorBoundary>
            </Tabs.Panel>
          )}
          <Tabs.Panel value="personvern">
            <InlineErrorBoundary>
              <AvtalePersonvern avtale={avtale} />
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <InlineErrorBoundary>
              <RedaksjoneltInnholdPreview
                tiltakstype={avtale.tiltakstype}
                beskrivelse={avtale.beskrivelse}
                faneinnhold={avtale.faneinnhold}
              />
            </InlineErrorBoundary>
          </Tabs.Panel>
        </Tabs>
      </WhitePaddedBox>
    </div>
  );
}
