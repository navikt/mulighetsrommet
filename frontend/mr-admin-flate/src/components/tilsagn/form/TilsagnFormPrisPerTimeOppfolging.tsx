import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { GjennomforingDto, TilsagnRequest } from "@mr/api-client-v2";
import { HGrid, Textarea, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { Metadata } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { InferredTilsagn } from "./TilsagnSchema";
import { useFindAvtaltSats } from "@/api/tilsagn/useFindAvtaltSats";

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  regioner: string[];
}

export function TilsagnFormPrisPerTimeOppfolging(props: Props) {
  return (
    <TilsagnForm
      {...props}
      beregningInput={<BeregningInputSkjema gjennomforing={props.gjennomforing} />}
    />
  );
}

function BeregningInputSkjema({ gjennomforing }: Pick<Props, "gjennomforing">) {
  const {
    register,
    formState: { errors },
    watch,
    getValues,
  } = useFormContext<InferredTilsagn>();

  const periodeStart = watch("periodeStart");
  const sats = useFindAvtaltSats(gjennomforing.avtaleId!, periodeStart);

  const type = getValues("beregning.type");
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
        value={prisbetingelser ?? ""}
        readOnly
      />
      <HGrid columns={2}>
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.antallPlasser.label}
          style={{ width: "180px" }}
          error={errors.beregning?.antallPlasser?.message}
          {...register("beregning.antallPlasser", {
            setValueAs: (v) => (v === "" ? null : Number(v)),
          })}
        />
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.sats.label(type)}
          style={{ width: "180px" }}
          readOnly
          value={sats?.pris ?? 0}
        />
      </HGrid>
      <HGrid columns={2}>
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.antallTimerOppfolgingPerDeltaker.label}
          style={{ width: "180px" }}
          error={errors.beregning?.antallTimerOppfolgingPerDeltaker?.message}
          {...register("beregning.antallTimerOppfolgingPerDeltaker", {
            setValueAs: (v) => (v === "" ? null : Number(v)),
          })}
        />
      </HGrid>
    </VStack>
  );
}
