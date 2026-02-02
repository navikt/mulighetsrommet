import { Alert, List, Box } from "@navikt/ds-react";
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
        <Alert variant="warning">
          {tekster.bokmal.utbetaling.beregning.advarslerFinnes}
          <Box marginBlock="space-16" asChild>
            <List data-aksel-migrated-v8>
              {advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
              ))}
            </List>
          </Box>
        </Alert>
      )}
      {beregning.deltakelser && <DataDrivenTable data={beregning.deltakelser} />}
    </>
  );
}
