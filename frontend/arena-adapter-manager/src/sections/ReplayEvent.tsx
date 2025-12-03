import { Section } from "../components/Section";
import { replayEvent } from "../core/api";
import { useState } from "react";
import { useArenaTables } from "../core/hooks";
import { Select, Button, TextField, HStack } from "@navikt/ds-react";

function ReplayEvent() {
  const { arenaTables, isArenaTablesLoading } = useArenaTables();
  const [arenaId, setArenaId] = useState<string>("");
  const [table, setTable] = useState<string>("");
  const [loading, setLoading] = useState(false);

  const handleReplay = async (table: string, arenaIdInput: string) => {
    setLoading(true);
    const ids = arenaIdInput
      .split(/[,\s]/)
      .map((id) => id.trim())
      .filter((id) => id.length);
    for (const id of ids) {
      await replayEvent(table, id);
    }
    setLoading(false);
  };

  return (
    <Section headerText="Replay Event" loadingText={"Laster"} isLoading={isArenaTablesLoading}>
      <Select
        label="tabell"
        hideLabel
        value={table}
        onChange={({ currentTarget }) => {
          setTable(currentTarget.value);
        }}
      >
        <option value="">Velg tabell</option>
        {arenaTables.map((table) => (
          <option key={table} value={table}>
            {table}
          </option>
        ))}
      </Select>
      <TextField
        placeholder="Arena-id eller kommaseparert liste med Arena-id'er"
        value={arenaId}
        onChange={({ currentTarget }) => {
          setArenaId(currentTarget.value);
        }}
        label="Arena ID"
        hideLabel
      />
      <HStack align="start">
        <Button disabled={loading} onClick={() => handleReplay(table, arenaId)}>
          {loading ? "Replaying event" : "Replay Event"}
        </Button>
      </HStack>
    </Section>
  );
}

export default ReplayEvent;
