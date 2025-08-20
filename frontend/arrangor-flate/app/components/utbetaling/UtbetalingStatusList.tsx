import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Accordion, BodyShort, Heading, Link, VStack } from "@navikt/ds-react";
import {
  ArrangorUtbetalingLinje,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
} from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { Definisjonsliste } from "../common/Definisjonsliste";
import { DelUtbetalingStatusTag } from "./DelUtbetalingStatusTag";
import { UtbetalingStatusTag } from "./UtbetalingStatusTag";
import { useOrgnrFromUrl } from "~/utils/navigation";
import { formaterDato } from "@mr/frontend-common/utils/date";

interface Props {
  utbetaling: ArrangorflateUtbetalingDto;
}

const seesPaaSomUtbetalt = (status: ArrangorflateUtbetalingStatus) =>
  [
    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    ArrangorflateUtbetalingStatus.UTBETALT,
  ].includes(status);

export default function UtbetalingStatusList({ utbetaling }: Props) {
  const erUtbetalt = seesPaaSomUtbetalt(utbetaling.status);

  return (
    <VStack gap="4">
      <Definisjonsliste
        title="Utbetalingsstatus"
        headingLevel="3"
        definitions={[
          {
            key: "Status",
            value: <UtbetalingStatusTag status={utbetaling.status} size={"small"} />,
          },
        ]}
      />

      {erUtbetalt && utbetaling.linjer.length > 0 ? (
        <>
          <Heading size="small" level="4">
            Tilsagn som er brukt til utbetaling
          </Heading>
          <UtbetalingTilsagndetaljer linjer={utbetaling.linjer} />
          <BodyShort>
            Godkjent beløp til utbetaling:{" "}
            <b>{formaterNOK(utbetaling.linjer.reduce((acc, cur) => cur.belop + acc, 0))}</b>
          </BodyShort>
        </>
      ) : null}
    </VStack>
  );
}

function UtbetalingTilsagndetaljer({ linjer }: { linjer: ArrangorUtbetalingLinje[] }) {
  const orgnr = useOrgnrFromUrl();

  return (
    <Accordion>
      {linjer
        .sort((linjeA, linjeB) =>
          linjeA.tilsagn.bestillingsnummer.localeCompare(linjeB.tilsagn.bestillingsnummer),
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
