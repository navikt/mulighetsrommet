import { Alert, Box, Heading } from "@navikt/ds-react";
import { Separator } from "~/components/Separator";
import { ArrangorflateTilsagn, ArrFlateUtbetaling } from "api-client";
import { TilsagnDetaljer } from "~/components/tilsagn/TilsagnDetaljer";
import { GenerelleDetaljer } from "~/components/utbetaling/GenerelleDetaljer";
import { formaterDato, formaterPeriode } from "~/utils";
import { Definisjonsliste } from "../Definisjonsliste";
import { BeregningDetaljer } from "./BeregningDetaljer";

interface Props {
  utbetaling: ArrFlateUtbetaling;
  tilsagn: ArrangorflateTilsagn[];
}

export function UtbetalingDetaljer({ utbetaling, tilsagn }: Props) {
  return (
    <>
      <GenerelleDetaljer utbetaling={utbetaling} />
      <Separator />
      <Heading size="medium">Tilsagnsdetaljer</Heading>
      {tilsagn.length === 0 && (
        <Alert variant="info">Det finnes ingen tilsagn for utbetalingsperioden</Alert>
      )}
      {tilsagn.map((t) => (
        <Box
          padding="2"
          key={t.id}
          borderWidth="1"
          borderColor="border-subtle"
          borderRadius="medium"
        >
          <TilsagnDetaljer tilsagn={t} />
        </Box>
      ))}
      <Separator />
      <Definisjonsliste
        title="Utbetaling"
        definitions={[
          {
            key: "Utbetalingsperiode",
            value: formaterPeriode(utbetaling.periode),
          },
          { key: "Frist for innsending", value: formaterDato(utbetaling.fristForGodkjenning) },
        ]}
      />
      <BeregningDetaljer beregning={utbetaling.beregning} />
    </>
  );
}
