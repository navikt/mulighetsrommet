import { Alert, List } from "@navikt/ds-react";
import { ArrangorflateBeregning, DeltakerAdvarsel } from "api-client";
import { tekster } from "~/tekster";
import { DataDrivenTable } from "../table/DataDrivenTable";

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
        <Alert variant="warning">
          {tekster.bokmal.utbetaling.beregning.advarslerFinnes}
          <List>
            {advarsler.map((advarsel) => (
              <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
            ))}
          </List>
        </Alert>
      )}
      {beregning.deltakelser && <DataDrivenTable data={beregning.deltakelser} />}
    </>
  );
}
