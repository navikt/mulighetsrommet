import { TilsagnBeregningFri, GjennomforingDto } from "@mr/api-client";
import { TilsagnSkjema } from "@/components/tilsagn/prismodell/TilsagnSkjema";
import { DeepPartial, useFormContext } from "react-hook-form";
import { TextField } from "@navikt/ds-react";
import { TilsagnBeregningPreview } from "@/components/tilsagn/prismodell/TilsagnBeregningPreview";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";

type FriTilsagn = InferredTilsagn & { beregning: TilsagnBeregningFri };

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<FriTilsagn>;
  defaultKostnadssteder: string[];
}

export function TilsagnSkjemaFri(props: Props) {
  return (
    <TilsagnSkjema
      {...props}
      beregningInput={<BeregningInputSkjema />}
      beregningOutput={<BeregningOutputPreview />}
    />
  );
}

function BeregningInputSkjema() {
  const {
    register,
    formState: { errors },
  } = useFormContext<FriTilsagn>();

  return (
    <TextField
      size="small"
      type="number"
      label="Beløp"
      style={{ width: "180px" }}
      error={errors.beregning?.belop?.message}
      {...register("beregning.belop", { valueAsNumber: true })}
    />
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<FriTilsagn>();

  const values = watch();
  return (
    <TilsagnBeregningPreview
      input={{
        type: "FRI",
        belop: values.beregning?.belop,
      }}
    />
  );
}
