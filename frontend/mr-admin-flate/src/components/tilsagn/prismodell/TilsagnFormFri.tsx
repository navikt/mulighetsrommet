import { TilsagnBeregningFri, GjennomforingDto } from "@mr/api-client-v2";
import { TilsagnForm } from "@/components/tilsagn/prismodell/TilsagnForm";
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
  regioner: string[];
}

export function TilsagnFormFri(props: Props) {
  return (
    <TilsagnForm
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
