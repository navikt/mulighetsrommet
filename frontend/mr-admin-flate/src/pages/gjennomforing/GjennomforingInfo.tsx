import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { useLoaderData } from "react-router";
import { GjennomforingDetaljer } from "./GjennomforingDetaljer";
import { GjennomforingKnapperad } from "./GjennomforingKnapperad";
import { gjennomforingLoader } from "./gjennomforingLoaders";

export function GjennomforingInfo() {
  const { gjennomforing, ansatt, avtale } = useLoaderData<typeof gjennomforingLoader>();

  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  return (
    <div data-testid="tiltaksgjennomforing_info-container">
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
