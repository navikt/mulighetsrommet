import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Accordion, Heading } from "@navikt/ds-react";
import { ArrangorUtbetalingLinje, ArrFlateUtbetaling, ArrFlateUtbetalingStatus } from "api-client";
import { Link } from "react-router";
import { formaterDato, useOrgnrFromUrl } from "../../utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { DelUtbetalingStatusTag } from "./DelUtbetalingStatusTag";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";

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
                  key: "Tilsagn",
                  value: (
                    <Link to={`/${orgnr}/tilsagn/${linje.tilsagn.id}`}>
                      Se tilsagnsdetaljer for {linje.tilsagn.bestillingsnummer}
                    </Link>
                  ),
                },
                { key: "Beløp", value: formaterNOK(linje.belop) },
                {
                  key: "Status",
                  value: <DelUtbetalingStatusTag status={linje.status} size={"small"} />,
                },
                {
                  key: "Status endret",
                  value: linje.statusSistOppdatert ? formaterDato(linje.statusSistOppdatert) : "-",
                },
              ]}
            />
          </Accordion.Content>
        </Accordion.Item>
      ))}
    </Accordion>
  );
}
