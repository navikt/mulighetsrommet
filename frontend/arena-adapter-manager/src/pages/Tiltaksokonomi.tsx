import KafkaConsumersOverview from "../sections/KafkaConsumersOverview";
import { ApiBase } from "../core/api.tsx";
import FailedKafkaConsumerRecordsOverview from "../sections/FailedKafkaConsumerRecordsOverview.tsx";

export function Tiltaksokonomi() {
  return (
    <>
      <KafkaConsumersOverview base={ApiBase.TILTAKSOKONOMI} />
      <FailedKafkaConsumerRecordsOverview base={ApiBase.TILTAKSOKONOMI} />
    </>
  );
}
