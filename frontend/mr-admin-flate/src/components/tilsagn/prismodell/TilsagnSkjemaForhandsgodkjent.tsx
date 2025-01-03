import { useFindForhandsgodkjentSats } from "@/api/tilsagn/useFindForhandsgodkjentSats";
import { TilsagnBeregningPreview } from "@/components/tilsagn/prismodell/TilsagnBeregningPreview";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { TilsagnSkjema } from "@/components/tilsagn/prismodell/TilsagnSkjema";
import { TilsagnBeregningForhandsgodkjent, TiltaksgjennomforingDto } from "@mr/api-client";
import { HGrid, TextField } from "@navikt/ds-react";
import { useEffect } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";

type ForhandsgodkjentTilsagn = InferredTilsagn & { beregning: TilsagnBeregningForhandsgodkjent };

interface Props {
  gjennomforing: TiltaksgjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<ForhandsgodkjentTilsagn>;
  defaultKostnadssteder: string[];
}

export function TilsagnSkjemaForhandsgodkjent(props: Props) {
  return (
    <TilsagnSkjema
      {...props}
      beregningInput={<BeregningInputSkjema gjennomforing={props.gjennomforing} />}
      beregningOutput={<BeregningOutputPreview />}
    />
  );
}

function BeregningInputSkjema({ gjennomforing }: Pick<Props, "gjennomforing">) {
  const {
    register,
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<ForhandsgodkjentTilsagn>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const sats = useFindForhandsgodkjentSats(gjennomforing.tiltakstype.tiltakskode, periodeStart);
  useEffect(() => {
    // FIXME: Satt til 0 for at validering og beregning ikke skal stoppe opp. Kan det gjøres på en bedre måte?
    const pris = sats?.pris ?? 0;
    setValue("beregning.sats", pris);
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
  const { watch } = useFormContext<ForhandsgodkjentTilsagn>();

  const values = watch();
  return (
    <TilsagnBeregningPreview
      input={{
        type: "FORHANDSGODKJENT",
        periodeStart: values.periodeStart,
        periodeSlutt: values.periodeSlutt,
        sats: values.beregning?.sats,
        antallPlasser: values.beregning?.antallPlasser,
      }}
    />
  );
}
