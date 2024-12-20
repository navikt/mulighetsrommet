import { TilsagnBeregningAft, TiltaksgjennomforingDto } from "@mr/api-client";
import { HGrid, TextField } from "@navikt/ds-react";
import { TilsagnSkjema } from "@/components/tilsagn/prismodell/TilsagnSkjema";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial, useFormContext } from "react-hook-form";
import { useFindAFTSatsForPeriode } from "@/api/tilsagn/useFindAFTSatsForPeriode";
import { useEffect } from "react";
import { TilsagnBeregningPreview } from "@/components/tilsagn/prismodell/TilsagnBeregningPreview";

type AftTilsagn = InferredTilsagn & { beregning: TilsagnBeregningAft };

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<AftTilsagn>;
  defaultKostnadssteder: string[];
}

export function AftTilsagnSkjema(props: Props) {
  return (
    <TilsagnSkjema
      {...props}
      renderBeregningInput={() => <BeregningInputSkjema />}
      renderBeregningOutput={() => <BeregningOutputPreview />}
    />
  );
}

function BeregningInputSkjema() {
  const {
    register,
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<AftTilsagn>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const { findSats } = useFindAFTSatsForPeriode();
  const sats = findSats(new Date(periodeStart));
  useEffect(() => {
    setValue("beregning.sats", sats ?? 0);
  }, [sats, setValue]);

  useEffect(() => {
    setValue("beregning.periodeStart", periodeStart);
  }, [periodeStart, setValue]);

  useEffect(() => {
    setValue("beregning.periodeSlutt", periodeSlutt);
  }, [periodeSlutt, setValue]);

  return (
    <HGrid columns={2}>
      <TextField
        size="small"
        type="number"
        label="Antall plasser"
        style={{ width: "180px" }}
        error={errors.beregning?.antallPlasser?.message}
        {...register("beregning.antallPlasser", { valueAsNumber: true })}
      />
      <TextField
        size="small"
        type="number"
        label="Sats"
        style={{ width: "180px" }}
        readOnly={true}
        error={errors.beregning?.sats?.message}
        {...register("beregning.sats", { valueAsNumber: true })}
      />
    </HGrid>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<AftTilsagn>();

  const values = watch();
  return (
    <TilsagnBeregningPreview
      input={{
        type: "AFT",
        periodeStart: values.periodeStart,
        periodeSlutt: values.periodeSlutt,
        sats: values.beregning?.sats,
        antallPlasser: values.beregning?.antallPlasser,
      }}
    />
  );
}
