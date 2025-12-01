import { useState } from "react";
import { Tabs } from "@navikt/ds-react";
import { TiltakstyperForm } from "./TiltakstyperForm";
import { TextInputForm } from "./TextInputForm";

interface TiltakstyperOrIdsFormProps {
  onSubmit: (data: { tiltakstyper?: string[]; id?: string }) => void;
  loading: boolean;
}

export function TiltakstyperOrIdsForm({ onSubmit, loading }: TiltakstyperOrIdsFormProps) {
  const [activeTab, setActiveTab] = useState("tiltakstyper");

  return (
    <Tabs value={activeTab} onChange={setActiveTab}>
      <Tabs.List>
        <Tabs.Tab value="tiltakstyper" label="Initial load basert på tiltakstyper" />
        <Tabs.Tab value="ids" label="Send ny melding basert på id" />
      </Tabs.List>
      <Tabs.Panel value="tiltakstyper">
        <TiltakstyperForm onSubmit={onSubmit} loading={loading} />
      </Tabs.Panel>
      <Tabs.Panel value="ids">
        <TextInputForm
          label="ID til gjennomføring"
          description="Flere id'er kan separeres med et komma (,)"
          name="id"
          onSubmit={onSubmit}
          loading={loading}
        />
      </Tabs.Panel>
    </Tabs>
  );
}
