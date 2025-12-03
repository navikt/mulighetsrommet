import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";
import ReplayEvent from "../sections/ReplayEvent";
import DeleteEvents from "../sections/DeleteEvents";
import { ApiBase } from "../core/api.tsx";

export function ArenaAdapter() {
  return (
    <>
      <TopicOverview base={ApiBase.ARENA_ADAPTER} />
      <ReplayEvents />
      <ReplayEvent />
      <DeleteEvents />
    </>
  );
}
