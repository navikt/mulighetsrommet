import { BodyShort, List, InfoCard } from "@navikt/ds-react";
import { ArrangorflateBeregning, DeltakerAdvarselDto } from "api-client";
import { tekster } from "~/tekster";
import { DataDrivenTable } from "@mr/frontend-common";

export function DeltakelserTable({
  beregning,
  advarsler,
}: {
  beregning: ArrangorflateBeregning;
  advarsler: DeltakerAdvarselDto[];
  deltakerlisteUrl: string;
}) {
  return (
    <>
      {advarsler.length > 0 && (
        <InfoCard data-color="warning">
          <InfoCard.Header>
            <InfoCard.Title>Viktig informasjon om deltakere</InfoCard.Title>
          </InfoCard.Header>
          <InfoCard.Content>
            <BodyShort spacing>{tekster.bokmal.utbetaling.beregning.advarslerFinnes}</BodyShort>
            <List data-aksel-migrated-v8>
              {advarsler.map((advarsel) => (
                <List.Item key={advarsel.deltakerId}>{advarsel.beskrivelse}</List.Item>
              ))}
            </List>
          </InfoCard.Content>
        </InfoCard>
      )}
      {beregning.deltakelser && <DataDrivenTable data={beregning.deltakelser} />}
    </>
  );
}
