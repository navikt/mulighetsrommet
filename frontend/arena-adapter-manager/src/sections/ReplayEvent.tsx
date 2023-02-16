import { Section } from "../components/Section";
import { Button, Input, Select } from "@chakra-ui/react";
import { replayEvent } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";

function ReplayEvent() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [arenaId, setArenaId] = useState<string>("");
  const [table, setTable] = useState<string>("");

  return (
    <Section
      headerText="Replay Event"
      loadingText={"Laster"}
      isLoading={isArenaTablesLoading}
    >
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
      <Input
        placeholder="Arena id"
        value={arenaId}
        onChange={({ currentTarget }) => {
          setArenaId(currentTarget.value);
        }}
      />
      <Button onClick={() => replayEvent(table, arenaId)}>Replay Event</Button>
    </Section>
  );
}

export default ReplayEvent;
