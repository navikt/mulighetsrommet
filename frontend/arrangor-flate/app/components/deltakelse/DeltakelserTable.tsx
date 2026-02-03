import { BodyShort, List, LocalAlert } from "@navikt/ds-react";
import { ArrangorflateBeregning, DeltakerAdvarsel } from "api-client";
import { tekster } from "~/tekster";
import { DataDrivenTable } from "@mr/frontend-common";

export function DeltakelserTable({
  beregning,
  advarsler,
}: {
  beregning: ArrangorflateBeregning;
  advarsler: DeltakerAdvarsel[];
  deltakerlisteUrl: string;
}) {
  return (
    <>
      {advarsler.length > 0 && (
        <LocalAlert status="warning">
          <LocalAlert.Header>
            <LocalAlert.Title>Viktig informasjon om deltakere</LocalAlert.Title>
          </LocalAlert.Header>
          <LocalAlert.Content>
            <BodyShort spacing>{tekster.bokmal.utbetaling.beregning.advarslerFinnes}</BodyShort>
            <List data-aksel-migrated-v8>
              {advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
              ))}
            </List>
          </LocalAlert.Content>
        </LocalAlert>
      )}
      {beregning.deltakelser && <DataDrivenTable data={beregning.deltakelser} />}
    </>
  );
}
