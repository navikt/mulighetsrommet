import ReplayEvents from "../sections/ReplayEvents";
import KafkaConsumersOverview from "../sections/KafkaConsumersOverview";
import ReplayEvent from "../sections/ReplayEvent";
import DeleteEvents from "../sections/DeleteEvents";
import { ApiBase } from "../core/api.tsx";

export function ArenaAdapter() {
  return (
    <>
      <KafkaConsumersOverview base={ApiBase.ARENA_ADAPTER} />
      <ReplayEvents />
      <ReplayEvent />
      <DeleteEvents />
    </>
  );
}
