import KafkaConsumersOverview from "../sections/KafkaConsumersOverview";
import { ApiBase } from "../core/api.tsx";

export function Tiltakshistorikk() {
  return (
    <>
      <KafkaConsumersOverview base={ApiBase.TILTAKSHISTORIKK} />
    </>
  );
}
