import { Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../../components/detaljside/Header";
import { DetaljLayout } from "../../layouts/DetaljLayout";
import { Avtaleinfo } from "./Avtaleinfo";

export function DetaljerAvtalePage() {
  const { data: avtale } = useAvtale();
  const [tabValgt, setTabValgt] = useState("avtaleinfo");

  return (
    <div>
      <Header>{avtale?.navn ?? "..."}</Header>
      <main>
        <Tabs value={tabValgt} onChange={setTabValgt}>
          <Tabs.List>
            <Tabs.Tab value="avtaleinfo" label="Avtaleinfo" />
          </Tabs.List>
          <Tabs.Panel value="avtaleinfo" className="h-24 w-full bg-gray-50 p-4">
            <DetaljLayout>
              <Avtaleinfo />
            </DetaljLayout>
          </Tabs.Panel>
        </Tabs>
      </main>
    </div>
  );
}
