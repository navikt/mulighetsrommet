import { HStack, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredAFTBeregningSchema } from "./OpprettTilsagnSchema";
import { NumericFormat } from "react-number-format";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { useAFTSatser } from "@/api/tilsagn/useAFTSatser";
import { AFTSats } from "@mr/api-client";
import { useBeregnAFTTilsagn } from "@/api/tilsagn/useBeregnAFTTilsagn";

interface Props {
  defaultAntallPlasser?: number;
  onBelopChange: (a0?: number) => void;
  periodeStart?: string;
  periodeSlutt?: string;
}

export function AFTBeregningSkjema({
  defaultAntallPlasser,
  onBelopChange,
  periodeStart,
  periodeSlutt,
}: Props) {
  const { data: satser } = useAFTSatser();
  const mutation = useBeregnAFTTilsagn();
  const {
    setError,
    clearErrors,
    setValue,
    formState: { errors },
  } = useFormContext<DeepPartial<InferredAFTBeregningSchema>>();

  const [belop, setBelop] = useState<number | undefined>(undefined);
  const [antallPlasser, setAntallPlasser] = useState<number>(defaultAntallPlasser ?? 0);

  function findSats(): number | undefined {
    if (!periodeStart) {
      return;
    }
    const filteredData =
      satser
        ?.filter((sats: AFTSats) => new Date(sats.startDato) <= new Date(periodeStart))
        ?.sort(
          (a: AFTSats, b: AFTSats) =>
            new Date(b.startDato).getTime() - new Date(a.startDato).getTime(),
        ) ?? [];

    return filteredData[0]?.belop;
  }

  function belopChange(value?: number) {
    setBelop(value);
    onBelopChange(value);
  }

  useEffect(() => {
    const sats = findSats();
    if (sats && periodeStart && periodeSlutt && mutation) {
      mutation.mutate({
        periodeStart,
        periodeSlutt,
        sats,
        antallPlasser,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [antallPlasser, periodeStart, periodeSlutt, setValue]);

  useHandleApiUpsertResponse(
    mutation,
    (response) => {
      clearErrors();
      belopChange(response);
    },
    (validation) => {
      belopChange(undefined);
      validation.errors.forEach((error) => {
        setError(error.name as keyof InferredAFTBeregningSchema, {
          type: "custom",
          message: error.message,
        });
      });
    },
  );

  return (
    <HStack gap="2" align="start">
      <NumericFormat
        size="small"
        error={errors.antallPlasser?.message}
        label="Antall plasser"
        customInput={TextField}
        value={antallPlasser}
        valueIsNumericString
        thousandSeparator
        onValueChange={(e) => {
          const n = Number.parseInt(e.value);
          setAntallPlasser(isNaN(n) ? 0 : n);
        }}
      />
      <NumericFormat
        readOnly
        size="small"
        label="Sats"
        error={errors.sats?.message}
        customInput={TextField}
        value={findSats()}
        valueIsNumericString
        thousandSeparator=" "
        suffix=" kr"
      />
      <NumericFormat
        readOnly
        size="small"
        label="Beløp"
        customInput={TextField}
        value={belop ?? ""}
        valueIsNumericString
        thousandSeparator=" "
        suffix=" kr"
      />
    </HStack>
  );
}
