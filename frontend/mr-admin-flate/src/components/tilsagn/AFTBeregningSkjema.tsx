import { HStack, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { BeregnTilsagnService } from "@mr/api-client";
import { NumericFormat } from "react-number-format";

interface Props {
  defaultAntallPlasser?: number;
}

// TODO: Hent fra backend gitt periodeStart. Eller hent liste fra backend
const sats = 20205;

export function AFTBeregningSkjema({ defaultAntallPlasser }: Props) {
  const {
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<DeepPartial<InferredOpprettTilsagnSchema>>();

  const [antallPlasser, setAntallPlasser] = useState<number>(defaultAntallPlasser ?? 0);

  const periode = watch("periode");
  const beregning = watch("beregning");

  useEffect(() => {
    const getBeregning = async () => {
      if (periode?.start && periode.slutt) {
        const beregning = await BeregnTilsagnService.beregnTilsagn({
          requestBody: {
            type: "AFT",
            periodeStart: periode.start,
            periodeSlutt: periode.slutt,
            sats,
            antallPlasser,
          },
        });
        setValue("beregning", beregning);
      }
    };

    getBeregning();
  }, [antallPlasser, periode?.start, periode?.slutt]);

  return (
    <HStack gap="2">
      <NumericFormat
        size="small"
        error={errors.beregning?.message}
        label="Antall plasser"
        customInput={TextField}
        value={antallPlasser}
        valueIsNumericString
        thousandSeparator
        onValueChange={(e) => {
          setAntallPlasser(Number.parseInt(e.value));
        }}
      />
      <NumericFormat
        readOnly
        size="small"
        error={errors.beregning?.message}
        label="Beløp"
        customInput={TextField}
        value={beregning?.belop}
        valueIsNumericString
        thousandSeparator
        suffix=" kr"
      />
    </HStack>
  );
}
