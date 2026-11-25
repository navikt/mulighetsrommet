import KafkaConsumersOverview from "../sections/KafkaConsumersOverview";
import { ApiBase } from "../core/api.tsx";
import FailedKafkaConsumerRecordsOverview from "../sections/FailedKafkaConsumerRecordsOverview.tsx";

export function Tiltakshistorikk() {
  return (
    <>
      <KafkaConsumersOverview base={ApiBase.TILTAKSHISTORIKK} />
      <FailedKafkaConsumerRecordsOverview base={ApiBase.TILTAKSHISTORIKK} />
    </>
  );
}
