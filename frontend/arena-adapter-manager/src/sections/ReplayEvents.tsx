import { Section } from "../components/Section";
import { Button, Select } from "@chakra-ui/react";
import { replayEvents } from "../core/api";
import { useState } from "react";

function ReplayEvents() {
  const [table, setTable] = useState<string>("");
  const [status, setStatus] = useState<string>("");
  const startReplayingOfEvents = () => {
    console.log(table);
    console.log(status);
    const value = replayEvents(
      table != "" ? table : null,
      status != "" ? status : null
    );
    console.log(value);
  };

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
        <option value="SIAMO.TILTAK">TILTAK</option>
        <option value="SIAMO.TILTAKGJENNOMFORING">TILTAKGJENNOMFORING</option>
        <option value="SIAMO.SAK">SAK</option>
        <option value="SIAMO.TILTAKDELTAKER">TILTAKDELTAKER</option>
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
      <Button onClick={() => startReplayingOfEvents()}>Replay Events</Button>
    </Section>
  );
}

export default ReplayEvents;

type ArenaTables =
  | "SIAMO.TILTAK"
  | "SIAMO.TILTAKGJENNOMFORING"
  | "SIAMO.SAK"
  | "SIAMO.TILTAKDELTAKER";

type ConsumptionStatus =
  | "Pending"
  | "Processed"
  | "Failed"
  | "Ignored"
  | "Invalid";
