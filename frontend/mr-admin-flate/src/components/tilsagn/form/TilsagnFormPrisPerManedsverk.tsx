import { useFindAvtaltSats } from "@/api/tilsagn/useFindAvtaltSats";
import { TilsagnBeregningPreview } from "@/components/tilsagn/form/TilsagnBeregningPreview";
import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { InferredTilsagn } from "@/components/tilsagn/form/TilsagnSchema";
import {
  GjennomforingDto,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { HGrid, Textarea, TextField, VStack } from "@navikt/ds-react";
import { useEffect } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { addDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { Metadata } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";

type TilsagnPrisPerManedsverk = InferredTilsagn & {
  beregning: TilsagnBeregningPrisPerManedsverk | TilsagnBeregningPrisPerUkesverk;
};

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<TilsagnPrisPerManedsverk>;
  regioner: string[];
}

export function TilsagnFormPrisPerManedsverk(props: Props) {
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
    getValues,
    setError,
    formState: { errors },
  } = useFormContext<TilsagnPrisPerManedsverk>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const sats = useFindAvtaltSats(gjennomforing.avtaleId!, periodeStart);
  const type = getValues("beregning.type");

  useEffect(() => {
    // FIXME: Satt til 0 for at validering og beregning ikke skal stoppe opp. Kan det gjøres på en bedre måte?
    if (sats?.pris) {
      setValue("beregning.sats", sats.pris);
    } else {
      setValue("beregning.sats", 0);
      setError("beregning.periode.start", { message: "Det finnes ingen sats for perioden" });
    }
  }, [sats, periodeStart, setValue, setError]);

  useEffect(() => {
    setValue("beregning.periode.start", periodeStart);
  }, [periodeStart, setValue]);

  useEffect(() => {
    const periodeSluttExclusive =
      yyyyMMddFormatting(addDuration(periodeSlutt, { days: 1 })) ?? periodeSlutt;
    setValue("beregning.periode.slutt", periodeSluttExclusive);
  }, [periodeSlutt, setValue]);

  const prisbetingelser = watch("beregning.prisbetingelser");

  return (
    <VStack gap="4">
      <Metadata
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(type)}
      />
      <Textarea
        size="small"
        label={avtaletekster.prisOgBetalingLabel}
        value={prisbetingelser || "-"}
        readOnly
      />
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
          label={tilsagnTekster.pris.label}
          style={{ width: "180px" }}
          readOnly={true}
          error={errors.beregning?.sats?.message}
          {...register("beregning.sats", { valueAsNumber: true })}
        />
      </HGrid>
    </VStack>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<TilsagnPrisPerManedsverk>();
  const values = watch("beregning");
  return <TilsagnBeregningPreview input={values} />;
}
