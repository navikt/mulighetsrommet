import { Section } from "../components/Section";
import { Button, Select } from "@chakra-ui/react";
import { replayEvents } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";

function ReplayEvents() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [status, setStatus] = useState<string>("");
  const [table, setTable] = useState<string>("");

  return (
    <Section headerText="Replay Events" loadingText={"Laster"} isLoading={isArenaTablesLoading}>
      <Select
        placeholder="Velg tabell"
        value={table}
        onChange={({ currentTarget }) => {
          setTable(currentTarget.value);
        }}
      >
        {arenaTables.map((table) => (
          <option key={table} value={table}>
            {table}
          </option>
        ))}
      </Select>
      <Select
        value={status}
        placeholder={"Alle statuser"}
        onChange={({ currentTarget }) => {
          setStatus(currentTarget.value);
        }}
      >
        <option value="Handled">Handled</option>
        <option value="Ignored">Ignored</option>
        <option value="Unhandled">Unhandled</option>
      </Select>
      <Button onClick={() => replayEvents(table, status != "" ? status : null)}>
        Replay Events
      </Button>
    </Section>
  );
}

export default ReplayEvents;
