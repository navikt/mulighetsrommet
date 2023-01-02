import { Section } from "../components/Section";
import { Button, Select } from "@chakra-ui/react";
import { replayEvents } from "../core/api";
import { useState } from "react";

export const arenatabeller = [
  "SIAMO.TILTAK",
  "SIAMO.TILTAKGJENNOMFORING",
  "SIAMO.SAK",
  "SIAMO.TILTAKDELTAKER",
] as const;

function ReplayEvents() {
  const [table, setTable] = useState<string>("");
  const [status, setStatus] = useState<string>("");

  return (
    <Section
      headerText="Replay Events"
      loadingText={"Laster"}
      isLoading={false}
    >
      <Select
        placeholder={"Alle arenatabeller"}
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
      <Select
        value={status}
        placeholder={"Alle statuser"}
        onChange={({ currentTarget }) => {
          setStatus(currentTarget.value);
        }}
      >
        <option value="Pending">Pending</option>
        <option value="Processed">Processed</option>
        <option value="Failed">Failed</option>
        <option value="Ignored">Ignored</option>
        <option value="Invalid">Invalid</option>
      </Select>
      <Button
        onClick={() =>
          replayEvents(table != "" ? table : null, status != "" ? status : null)
        }
      >
        Replay Events
      </Button>
    </Section>
  );
}

export default ReplayEvents;
