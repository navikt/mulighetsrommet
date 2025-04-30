import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Accordion, Heading } from "@navikt/ds-react";
import {
  ArrangorflateTilsagn,
  ArrangorUtbetalingLinje,
  ArrFlateUtbetaling,
  ArrFlateUtbetalingStatus,
} from "api-client";
import { Link } from "react-router";
import { useOrgnrFromUrl } from "../../utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { DelUtbetalingStatusTag } from "./DelUtbetalingStatusTag";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

export default function UtbetalingStatusList({ utbetaling }: Props) {
  return (
    <>
      <Definisjonsliste
        title="Utbetalingsstatus"
        definitions={[
          {
            key: "Status",
            value: <UtbetalingStatusTag status={utbetaling.status} size={"small"} />,
          },
        ]}
      />

      {utbetaling.status === ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING &&
      utbetaling.linjer &&
      utbetaling.linjer.length > 0 ? (
        <>
          <Heading size="small" level="3">
            Tilsagnsdetaljer
          </Heading>
          <Accordion>
            <UtbetalingTilsagndetaljer linjer={utbetaling.linjer} />
          </Accordion>
        </>
      ) : null}
    </>
  );
}

function UtbetalingTilsagndetaljer({ linjer }: { linjer: ArrangorUtbetalingLinje[] }) {
  const orgnr = useOrgnrFromUrl();
  return (
    <Accordion>
      {linjer.map((linje) => (
        <Accordion.Item key={linje.id} defaultOpen={linjer.length === 1}>
          <Accordion.Header>Tilsagn {linje.tilsagn.bestillingsnummer} </Accordion.Header>
          <Accordion.Content>
            <Definisjonsliste
              definitions={[
                {
                  key: "Tilsagnsnummer",
                  value: (
                    <Link to={`/${orgnr}/tilsagn/${linje.tilsagn.id}`}>
                      {linje.tilsagn.bestillingsnummer}
                    </Link>
                  ),
                },
                { key: "Beløp", value: formaterNOK(linje.belop) },
                {
                  key: "Status",
                  value: <DelUtbetalingStatusTag status={linje.status} size={"small"} />,
                },
              ]}
            />
          </Accordion.Content>
        </Accordion.Item>
      ))}
    </Accordion>
  );
}
