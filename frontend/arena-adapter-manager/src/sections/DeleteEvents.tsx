import { Section } from "../components/Section";
import { Button, Input, Select } from "@chakra-ui/react";
import { deleteEvents } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";

function DeleteEvents() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [arenaIds, setArenaIds] = useState<string>("");
  const [table, setTable] = useState<string>("");

  return (
    <Section headerText="Delete Events" loadingText={"Laster"} isLoading={isArenaTablesLoading}>
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
        placeholder="Arena-idene på eventene som skal settes separert med komma"
        value={arenaIds}
        onChange={({ currentTarget }) => {
          setArenaIds(currentTarget.value);
        }}
      />
      <Button onClick={() => deleteEvents(table, arenaIds)}>Delete Events</Button>
    </Section>
  );
}

export default DeleteEvents;
