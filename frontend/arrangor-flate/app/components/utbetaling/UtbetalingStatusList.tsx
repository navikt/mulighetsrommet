import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Accordion, BodyShort, Heading, Link, VStack } from "@navikt/ds-react";
import {
  ArrangforflateUtbetalingLinje,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingStatus,
} from "api-client";
import { Link as ReactRouterLink } from "react-router";
import { Definisjonsliste, Definition } from "../common/Definisjonsliste";
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
  const godkjentBelop = {
    valuta: utbetaling.valuta,
    belop: utbetaling.linjer.reduce((acc, cur) => cur.pris.belop + acc, 0),
  };

  const avbruttDato: Definition[] = utbetaling.avbruttDato
    ? [{ key: "Avbrutt dato", value: formaterDato(utbetaling.avbruttDato) }]
    : [];

  return (
    <VStack gap="space-16">
      <Definisjonsliste
        title="Utbetalingsstatus"
        definitions={[
          {
            key: "Status",
            value: <UtbetalingStatusTag status={utbetaling.status} size={"small"} />,
          },
          ...avbruttDato,
        ]}
      />
      {erUtbetalt && utbetaling.linjer.length > 0 ? (
        <>
          <Heading size="small" level="4">
            Tilsagn som er brukt til utbetaling
          </Heading>
          <UtbetalingTilsagndetaljer linjer={utbetaling.linjer} />
          <BodyShort>
            Godkjent beløp til utbetaling: <b>{formaterValutaBelop(godkjentBelop)}</b>
          </BodyShort>
        </>
      ) : null}
    </VStack>
  );
}

function UtbetalingTilsagndetaljer({ linjer }: { linjer: ArrangforflateUtbetalingLinje[] }) {
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
                  { key: "Beløp til utbetaling", value: formaterValutaBelop(linje.pris) },
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
