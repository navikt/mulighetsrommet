import { Section } from "../components/Section";
import { deleteEvents } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";
import { Select, Button, TextField, HStack } from "@navikt/ds-react";

function DeleteEvents() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [arenaIds, setArenaIds] = useState<string>("");
  const [table, setTable] = useState<string>("");

  return (
    <Section headerText="Delete Events" loadingText={"Laster"} isLoading={isArenaTablesLoading}>
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
      <TextField
        placeholder="Arena-idene på eventene som skal settes separert med komma"
        value={arenaIds}
        onChange={({ currentTarget }) => {
          setArenaIds(currentTarget.value);
        }}
        label={"Arena id-er"}
        hideLabel
      />
      <HStack align="start">
        <Button onClick={() => deleteEvents(table, arenaIds)}>Delete Events</Button>
      </HStack>
    </Section>
  );
}

export default DeleteEvents;
