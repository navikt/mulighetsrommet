import TopicOverview from "../sections/TopicOverview";
import { ApiBase } from "../core/api.tsx";

export function Tiltaksokonomi() {
  return (
    <>
      <TopicOverview base={ApiBase.TILTAKSOKONOMI} />
    </>
  );
}
