import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import {
  GjennomforingDto,
  PrismodellDto,
  TilsagnBeregningType,
  TilsagnRequest,
} from "@tiltaksadministrasjon/api-client";
import { HGrid, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { useFindAvtaltSats } from "@/api/avtaler/useFindAvtaltSats";
import { MetadataVStack } from "@mr/frontend-common/components/datadriven/Metadata";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  kostnadssteder: KostnadsstedOption[];
}

export function TilsagnFormFastSatsPerTiltaksplassPerManed(props: Props) {
  return (
    <TilsagnForm
      gjennomforing={props.gjennomforing}
      kostnadssteder={props.kostnadssteder}
      onSuccess={props.onSuccess}
      onAvbryt={props.onAvbryt}
      defaultValues={props.defaultValues}
      beregningInput={<BeregningInputSkjema prismodell={props.prismodell} />}
    />
  );
}

function BeregningInputSkjema({ prismodell }: Pick<Props, "prismodell">) {
  const {
    register,
    formState: { errors },
    watch,
  } = useFormContext<TilsagnRequest>();

  const periodeStart = watch("periodeStart");
  const sats = useFindAvtaltSats(prismodell, periodeStart);

  return (
    <VStack gap="space-16">
      <MetadataVStack
        label={tilsagnTekster.prismodell.label}
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
        <VStack gap="space-8">
          <TextField
            size="small"
            type="number"
            label={tilsagnTekster.sats.label(
              TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            )}
            style={{ width: "180px" }}
            readOnly
            value={sats?.pris.belop ?? 0}
          />
        </VStack>
      </HGrid>
    </VStack>
  );
}
