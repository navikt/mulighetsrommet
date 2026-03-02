import TopicOverview from "../sections/TopicOverview";
import { ApiBase } from "../core/api.tsx";
import FailedKafkaConsumerRecordsOverview from "../sections/FailedKafkaConsumerRecordsOverview.tsx";

export function Tiltaksokonomi() {
  return (
    <>
      <TopicOverview base={ApiBase.TILTAKSOKONOMI} />
      <FailedKafkaConsumerRecordsOverview base={ApiBase.TILTAKSOKONOMI} />
    </>
  );
}
