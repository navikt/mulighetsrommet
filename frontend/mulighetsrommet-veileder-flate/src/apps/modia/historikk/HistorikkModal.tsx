import { Tabs } from "@navikt/ds-react";
import { HistorikkForBrukerModalInnhold } from "./HistorikkForBrukerModalInnhold";
import { DelMedBrukerHistorikk } from "../delMedBruker/DelMedBrukerHistorikk";
import { useLogEvent } from "../../../logging/amplitude";

export function HistorikkModal() {
  const { logEvent } = useLogEvent();
  return (
    <Tabs defaultValue="tiltakshistorikk">
      <Tabs.List>
        <Tabs.Tab
          value="tiltakshistorikk"
          label="Tiltakshistorikk"
          onClick={() =>
            logEvent({
              name: "arbeidsmarkedstiltak.historikk.fane-valgt",
              data: {
                action: "Tiltakshistorikk",
              },
            })
          }
        />
        <Tabs.Tab
          value="deltMedBruker"
          label="Delt med bruker"
          onClick={() =>
            logEvent({
              name: "arbeidsmarkedstiltak.historikk.fane-valgt",
              data: {
                action: "Delt med bruker",
              },
            })
          }
        />
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
