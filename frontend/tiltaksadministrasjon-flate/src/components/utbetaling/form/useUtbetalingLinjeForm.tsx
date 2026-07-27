import {
  OpprettUtbetalingLinjerRequest,
  UtbetalingDto,
  UtbetalingLinjeDto,
} from "@tiltaksadministrasjon/api-client";
import { useEffect } from "react";
import { useForm } from "react-hook-form";

export function useUtbetalingLinjeForm(
  utbetaling: UtbetalingDto,
  utbetalingLinjer: UtbetalingLinjeDto[],
) {
  const form = useForm<OpprettUtbetalingLinjerRequest>({
    defaultValues: mapToDefaultValues(utbetaling, utbetalingLinjer),
  });
  const { reset, setValue } = form;

  useEffect(() => {
    reset(mapToDefaultValues(utbetaling, utbetalingLinjer));
  }, [utbetaling, utbetalingLinjer, reset]);

  function hentGodkjenteTilsagn() {
    setValue("utbetalingLinjer", mapToDefaultValues(utbetaling, utbetalingLinjer).utbetalingLinjer);
  }

  return { form, hentGodkjenteTilsagn };
}

function mapToDefaultValues(
  utbetaling: UtbetalingDto,
  utbetalingLinjer: UtbetalingLinjeDto[],
): OpprettUtbetalingLinjerRequest {
  return {
    utbetalingId: utbetaling.id,
    utbetalingLinjer: utbetalingLinjer.map((linje) => ({
      gjorOppTilsagn: linje.gjorOppTilsagn,
      pris: linje.pris,
      id: linje.id,
      tilsagnId: linje.tilsagn.id,
    })),
    begrunnelseMindreBetalt: null,
  };
}
