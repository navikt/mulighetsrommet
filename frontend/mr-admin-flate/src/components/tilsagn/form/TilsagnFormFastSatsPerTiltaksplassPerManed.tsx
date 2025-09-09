import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { GjennomforingDto } from "@mr/api-client-v2";
import { TilsagnBeregningType, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { HGrid, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { Metadata } from "@/components/detaljside/Metadata";
import { useFindAvtaltSats } from "@/api/avtaler/useFindAvtaltSats";

interface Props {
  gjennomforing: GjennomforingDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  regioner: string[];
}

export function TilsagnFormFastSatsPerTiltaksplassPerManed(props: Props) {
  return (
    <TilsagnForm
      gjennomforing={props.gjennomforing}
      regioner={props.regioner}
      onSuccess={props.onSuccess}
      onAvbryt={props.onAvbryt}
      defaultValues={props.defaultValues}
      beregningInput={<BeregningInputSkjema gjennomforing={props.gjennomforing} />}
    />
  );
}

function BeregningInputSkjema({ gjennomforing }: Pick<Props, "gjennomforing">) {
  const {
    register,
    formState: { errors },
    watch,
  } = useFormContext<TilsagnRequest>();

  const periodeStart = watch("periodeStart");
  const sats = useFindAvtaltSats(gjennomforing.avtaleId ?? "", periodeStart);

  return (
    <VStack gap="4">
      <Metadata
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(
          TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
        )}
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
        <VStack gap="2">
          <TextField
            size="small"
            type="number"
            label={tilsagnTekster.sats.label(
              TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            )}
            style={{ width: "180px" }}
            readOnly
            value={sats?.pris ?? 0}
          />
        </VStack>
      </HGrid>
    </VStack>
  );
}
