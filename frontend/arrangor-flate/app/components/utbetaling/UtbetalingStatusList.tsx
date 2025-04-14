import { Definisjonsliste } from "../Definisjonsliste";
import { ArrFlateUtbetaling, ArrFlateUtbetalingStatus } from "api-client";
import { UtbetalingStatusTag } from "./UtbetalingTable";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

export default function UtbetalingStatusList({ utbetaling }: Props) {
  const placeholder = () =>
    utbetaling.status === ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV ? "-" : "@TODO";
  return (
    <Definisjonsliste
      title="Utbetalingsstatus"
      definitions={[
        {
          key: "Status",
          value: UtbetalingStatusTag(utbetaling.status, "small"),
        },
        // TODO:
        { key: "Dato", value: placeholder() },
        { key: "Utbetalt", value: placeholder() },
      ]}
    />
  );
}
