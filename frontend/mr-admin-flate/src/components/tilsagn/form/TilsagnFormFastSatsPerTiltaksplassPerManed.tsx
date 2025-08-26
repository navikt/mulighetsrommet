import { useFindAvtaltSats } from "@/api/tilsagn/useFindAvtaltSats";
import { TilsagnBeregningPreview } from "@/components/tilsagn/form/TilsagnBeregningPreview";
import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { InferredTilsagn } from "@/components/tilsagn/form/TilsagnSchema";
import {
  GjennomforingDto,
  TilsagnBeregningFastSatsPerTiltaksplassPerManed,
} from "@mr/api-client-v2";
import { HGrid, TextField, VStack } from "@navikt/ds-react";
import { useEffect } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { addDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { Metadata } from "@/components/detaljside/Metadata";

type TilsagnFastSats = InferredTilsagn & {
  beregning: TilsagnBeregningFastSatsPerTiltaksplassPerManed;
};

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: DeepPartial<TilsagnFastSats>;
  regioner: string[];
}

export function TilsagnFormFastSatsPerTiltaksplassPerManed(props: Props) {
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
  } = useFormContext<TilsagnFastSats>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const sats = useFindAvtaltSats(gjennomforing.avtaleId!, periodeStart);

  useEffect(() => {
    // FIXME: Satt til 0 for at validering og beregning ikke skal stoppe opp. Kan det gjøres på en bedre måte?
    const pris = sats?.pris ?? 0;
    setValue("beregning.sats", pris);
  }, [sats, setValue]);

  useEffect(() => {
    setValue("beregning.periode.start", periodeStart);
  }, [periodeStart, setValue]);

  useEffect(() => {
    const periodeSluttExclusive =
      yyyyMMddFormatting(addDuration(periodeSlutt, { days: 1 })) ?? periodeSlutt;
    setValue("beregning.periode.slutt", periodeSluttExclusive);
  }, [periodeSlutt, setValue]);

  return (
    <VStack gap="4">
      <Metadata
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label("FAST_SATS_PER_TILTAKSPLASS_PER_MANED")}
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
        <VStack gap="2">
          <TextField
            size="small"
            type="number"
            label={tilsagnTekster.sats.label}
            style={{ width: "180px" }}
            readOnly={true}
            error={errors.beregning?.sats?.message}
            {...register("beregning.sats", { valueAsNumber: true })}
          />
        </VStack>
      </HGrid>
    </VStack>
  );
}

function BeregningOutputPreview() {
  const { watch } = useFormContext<TilsagnFastSats>();
  const values = watch("beregning");
  return <TilsagnBeregningPreview input={values} />;
}
