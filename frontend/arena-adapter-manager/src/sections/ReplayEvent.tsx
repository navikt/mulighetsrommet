import { Section } from "../components/Section";
import { Button, Input, Select } from "@chakra-ui/react";
import { replayEvent, replayEvents } from "../core/api";
import { useState } from "react";
import { arenatabeller } from "./ReplayEvents";

function ReplayEvent() {
  const [arenaId, setArenaId] = useState<string>("");
  const [table, setTable] = useState<string>(arenatabeller[0]);

  return (
    <Section headerText="Replay Event" loadingText={"Laster"} isLoading={false}>
      <Select
        value={table}
        onChange={({ currentTarget }) => {
          setTable(currentTarget.value);
        }}
      >
        {arenatabeller.map((tabell) => (
          <option key={tabell} value={tabell}>
            {tabell}
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
