import TopicOverview from "../sections/TopicOverview";
import { ApiBase } from "../core/api.tsx";

export function Tiltakshistorikk() {
  return (
    <>
      <TopicOverview base={ApiBase.TILTAKSHISTORIKK} />
    </>
  );
}
