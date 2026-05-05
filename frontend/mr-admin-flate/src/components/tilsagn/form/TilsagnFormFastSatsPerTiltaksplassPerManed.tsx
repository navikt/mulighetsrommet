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
import { NumberInput } from "@/components/skjema/NumberInput";

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
  const { watch } = useFormContext<TilsagnRequest>();

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
      <HGrid gap="space-16" align="start" columns={2}>
        <NumberInput<TilsagnRequest>
          name="beregning.antallPlasser"
          label={tilsagnTekster.antallPlasser.label}
        />
        <VStack gap="space-8">
          <TextField
            size="small"
            type="number"
            label={tilsagnTekster.sats.label(
              TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
            )}
            readOnly
            value={sats?.pris.belop ?? 0}
          />
        </VStack>
      </HGrid>
    </VStack>
  );
}
