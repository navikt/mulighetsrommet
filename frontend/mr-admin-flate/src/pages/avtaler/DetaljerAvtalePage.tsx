import { Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../../components/detaljside/Header";
import { Avtalestatus } from "../../components/statuselementer/Avtalestatus";
import { DetaljLayout } from "../../layouts/DetaljLayout";
import { Avtaleinfo } from "./Avtaleinfo";

export function DetaljerAvtalePage() {
  const { data: avtale } = useAvtale();
  const [tabValgt, setTabValgt] = useState("avtaleinfo");

  if (!avtale) {
    return null;
  }

  return (
    <main>
      <Header>
        <div
          style={{
            display: "flex",
            gap: "1rem",
          }}
        >
          <span>{avtale?.navn ?? "..."}</span>
          <Avtalestatus avtale={avtale} />
        </div>{" "}
      </Header>
      <Tabs value={tabValgt} onChange={setTabValgt}>
        <Tabs.List style={{ paddingLeft: "4rem" }}>
          <Tabs.Tab value="avtaleinfo" label="Avtaleinfo" />
        </Tabs.List>
        <Tabs.Panel value="avtaleinfo" className="h-24 w-full bg-gray-50 p-4">
          <DetaljLayout>
            <Avtaleinfo />
          </DetaljLayout>
        </Tabs.Panel>
      </Tabs>
    </main>
  );
}
