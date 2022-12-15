import { Section } from "../components/Section";
import { Button } from "@chakra-ui/react";
import { replayEvents } from "../core/api";

function ReplayEvents() {
  const startReplayingOfEvents = () => {
    replayEvents();
  };

  return (
    <Section
      headerText="Replay Events"
      loadingText={"Laster"}
      isLoading={false}
    >
      <div>Hei</div>
      <Button onClick={() => startReplayingOfEvents()}>Replay Events</Button>
    </Section>
  );
}

export default ReplayEvents;
