import { StengtPeriode } from "@arrangor-utbetalinger/api-client";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { InfoCard, BodyShort } from "@navikt/ds-react";
import { tekster } from "~/tekster";

export function StengtePerioder({ perioder }: { perioder: StengtPeriode[] }) {
  return (
    <>
      <InfoCard data-color="info" size="small">
        <InfoCard.Header>
          <InfoCard.Title as="h4">Stengte perioder</InfoCard.Title>
        </InfoCard.Header>
        <InfoCard.Content>
          <BodyShort spacing>{tekster.bokmal.utbetaling.beregning.stengtHosArrangor}</BodyShort>
          <ul>
            {perioder.map(({ periode, beskrivelse }: StengtPeriode) => (
              <li key={periode.start + periode.slutt}>
                {formaterPeriode(periode)}: {beskrivelse}
              </li>
            ))}
          </ul>
        </InfoCard.Content>
      </InfoCard>
    </>
  );
}
