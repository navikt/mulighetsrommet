import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useParams } from "react-router";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { GjennomforingDetaljer } from "./GjennomforingDetaljer";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";

function useGjennomforingInfoData() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: ansatt } = useHentAnsatt();
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);

  return {
    gjennomforing,
    ansatt,
    avtale,
  };
}

export function GjennomforingInfo() {
  const { gjennomforing, ansatt, avtale } = useGjennomforingInfoData();

  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

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
          <GjennomforingKnapperad ansatt={ansatt} avtale={avtale} gjennomforing={gjennomforing} />
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
