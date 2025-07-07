import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Accordion, Heading, Link, VStack, HStack } from "@navikt/ds-react";
import { ArrangorUtbetalingLinje, ArrFlateUtbetaling, ArrFlateUtbetalingStatus } from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { formaterDato, useOrgnrFromUrl } from "../../utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { DelUtbetalingStatusTag } from "./DelUtbetalingStatusTag";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";

interface Props {
  utbetaling: ArrFlateUtbetaling;
}

const seesPaaSomUtbetalt = (status: ArrFlateUtbetalingStatus) =>
  [ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING, ArrFlateUtbetalingStatus.UTBETALT].includes(
    status,
  );

export default function UtbetalingStatusList({ utbetaling }: Props) {
  const erUtbetalt = seesPaaSomUtbetalt(utbetaling.status);

  return (
    <>
      <VStack gap="4">
        <Definisjonsliste
          title="Utbetalingstatus"
          definitions={[
            {
              key: "Status",
              value: <UtbetalingStatusTag status={utbetaling.status} size={"small"} />,
            },
          ]}
        />

        {erUtbetalt && utbetaling.linjer && utbetaling.linjer.length > 0 ? (
          <>
            <Heading size="small" level="3">
              Tilsagn som er brukt til utbetaling
            </Heading>
            <UtbetalingTilsagndetaljer linjer={utbetaling.linjer} />
            <UtbetalingTotaltBelop linjer={utbetaling.linjer} />
          </>
        ) : null}
      </VStack>
    </>
  );
}

function UtbetalingTotaltBelop({ linjer }: { linjer: ArrangorUtbetalingLinje[] }) {
  return (
    <HStack gap="2" align="center" className="mt-2">
      <div>Godkjent beløp til utbetaling:</div>
      <div className="font-bold text-right">
        {formaterNOK(linjer.reduce((acc, cur) => cur.belop + acc, 0))}
      </div>
    </HStack>
  );
}

function UtbetalingTilsagndetaljer({ linjer }: { linjer: ArrangorUtbetalingLinje[] }) {
  const orgnr = useOrgnrFromUrl();

  return (
    <Accordion>
      {linjer
        .sort((linjea, linjeb) =>
          linjea.tilsagn.bestillingsnummer.localeCompare(linjeb.tilsagn.bestillingsnummer),
        )
        .map((linje) => (
          <Accordion.Item key={linje.id}>
            <Accordion.Header>Tilsagn {linje.tilsagn.bestillingsnummer} </Accordion.Header>
            <Accordion.Content>
              <Definisjonsliste
                definitions={[
                  {
                    key: "Tilsagn",
                    value: (
                      <Link as={ReactRouterLink} to={`/${orgnr}/tilsagn/${linje.tilsagn.id}`}>
                        Se tilsagnsdetaljer
                      </Link>
                    ),
                  },
                  { key: "Beløp til utbetaling", value: formaterNOK(linje.belop) },
                  {
                    key: "Status",
                    value: <DelUtbetalingStatusTag status={linje.status} size={"small"} />,
                  },
                  {
                    key: "Status endret",
                    value: linje.statusSistOppdatert
                      ? formaterDato(linje.statusSistOppdatert)
                      : "-",
                  },
                ]}
              />
            </Accordion.Content>
          </Accordion.Item>
        ))}
    </Accordion>
  );
}
