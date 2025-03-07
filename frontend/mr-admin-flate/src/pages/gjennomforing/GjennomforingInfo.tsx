import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "../../api/gjennomforing/useAdminGjennomforingById";
import { GjennomforingDetaljer } from "./GjennomforingDetaljer";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";
import { Laster } from "../../components/laster/Laster";

function useGjennomforingInfoData() {
  const { data: gjennomforing } = useAdminGjennomforingById();
  const { data: ansatt } = useHentAnsatt();
  const { data: avtale } = useAvtale(gjennomforing?.avtaleId);

  return {
    gjennomforing,
    ansatt,
    avtale,
  };
}

export function GjennomforingInfo() {
  const { gjennomforing, ansatt, avtale } = useGjennomforingInfoData();

  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  if (!gjennomforing) {
    return <Laster tekst="Laster gjennomføring..." />;
  }

  return (
    <div data-testid="gjennomforing_info-container">
      <Tabs defaultValue={activeTab}>
        <Tabs.List className="flex flex-row justify-between">
          <div>
            <Tabs.Tab onClick={() => setActiveTab("detaljer")} value="detaljer" label="Detaljer" />
            <Tabs.Tab
              onClick={() => setActiveTab("redaksjonelt-innhold")}
              value="redaksjonelt-innhold"
              label="Redaksjonelt innhold"
            />
          </div>
          <GjennomforingKnapperad ansatt={ansatt} gjennomforing={gjennomforing} />
        </Tabs.List>
        <Tabs.Panel value="detaljer">
          <InlineErrorBoundary>
            <GjennomforingDetaljer gjennomforing={gjennomforing} avtale={avtale} />
          </InlineErrorBoundary>
        </Tabs.Panel>
        <Tabs.Panel value="redaksjonelt-innhold">
          <InlineErrorBoundary>
            <RedaksjoneltInnholdPreview
              tiltakstype={gjennomforing.tiltakstype}
              beskrivelse={gjennomforing.beskrivelse}
              faneinnhold={gjennomforing.faneinnhold}
            />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
