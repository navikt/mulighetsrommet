import { useFindForhandsgodkjentSats } from "@/api/tilsagn/useFindForhandsgodkjentSats";
import { TilsagnBeregningPreview } from "@/components/tilsagn/prismodell/TilsagnBeregningPreview";
import { TilsagnForm } from "@/components/tilsagn/prismodell/TilsagnForm";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { GjennomforingDto, TilsagnBeregningForhandsgodkjent } from "@mr/api-client-v2";
import { HGrid, TextField } from "@navikt/ds-react";
import { useEffect } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { addDays } from "@/utils/Utils";
import { tilsagnTekster } from "../TilsagnTekster";
import { formaterDatoSomYYYYMMDD } from "@mr/frontend-common/utils/date";

type ForhandsgodkjentTilsagn = InferredTilsagn & { beregning: TilsagnBeregningForhandsgodkjent };

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<ForhandsgodkjentTilsagn>;
  regioner: string[];
}

export function TilsagnFormForhandsgodkjent(props: Props) {
  return (
    <TilsagnForm
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
    setValue("beregning.periode.start", periodeStart);
  }, [periodeStart, setValue]);

  useEffect(() => {
    const periodeSluttExclusive = periodeSlutt
      ? formaterDatoSomYYYYMMDD(addDays(periodeSlutt, 1))
      : periodeSlutt;
    setValue("beregning.periode.slutt", periodeSluttExclusive);
  }, [periodeSlutt, setValue]);

  return (
    <HGrid columns={2}>
      <TextField
        size="small"
        type="number"
        label={tilsagnTekster.antallPlasser.label}
        style={{ width: "180px" }}
        error={errors.beregning?.antallPlasser?.message}
        {...register("beregning.antallPlasser", { valueAsNumber: true })}
      />
      <TextField
        size="small"
        type="number"
        label={tilsagnTekster.sats.label}
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
  const values = watch("beregning");
  return <TilsagnBeregningPreview input={values} />;
}
