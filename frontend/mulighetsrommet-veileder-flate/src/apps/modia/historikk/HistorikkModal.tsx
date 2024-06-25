import { Tabs } from "@navikt/ds-react";
import { HistorikkForBrukerModalInnhold } from "./HistorikkForBrukerModalInnhold";
import { DelMedBrukerHistorikk } from "../delMedBruker/DelMedBrukerHistorikk";

export function HistorikkModal() {
  return (
    <Tabs defaultValue="tiltakshistorikk">
      <Tabs.List>
        <Tabs.Tab value="tiltakshistorikk" label="Tiltakshistorikk" />
        <Tabs.Tab value="deltMedBruker" label="Delt med bruker" />
      </Tabs.List>
      <Tabs.Panel value="tiltakshistorikk">
        <HistorikkForBrukerModalInnhold />
      </Tabs.Panel>
      <Tabs.Panel value="deltMedBruker">
        <div style={{ marginTop: "1rem" }}>
          <DelMedBrukerHistorikk />
        </div>
      </Tabs.Panel>
    </Tabs>
  );
}
