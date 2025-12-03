import { Section } from "../components/Section";
import { replayEvents } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";
import { Select, Button, HStack } from "@navikt/ds-react";

function ReplayEvents() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [status, setStatus] = useState<string>("");
  const [table, setTable] = useState<string>("");

  return (
    <Section headerText="Replay Events" loadingText={"Laster"} isLoading={isArenaTablesLoading}>
      <Select
        value={table}
        onChange={({ currentTarget }) => {
          setTable(currentTarget.value);
        }}
        label="tabell"
        hideLabel
      >
        <option value="">Velg tabell</option>
        {arenaTables.map((table) => (
          <option key={table} value={table}>
            {table}
          </option>
        ))}
      </Select>
      <Select
        value={status}
        label={"Alle statuser"}
        onChange={({ currentTarget }) => {
          setStatus(currentTarget.value);
        }}
      >
        <option value="Handled">Handled</option>
        <option value="Ignored">Ignored</option>
        <option value="Unhandled">Unhandled</option>
      </Select>
      <HStack align="start">
        <Button onClick={() => replayEvents(table, status !== "" ? status : null)}>
          Replay Events
        </Button>
      </HStack>
    </Section>
  );
}

export default ReplayEvents;
