import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { PrismodellDto, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { HGrid, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { useFindAvtaltSats } from "@/api/avtaler/useFindAvtaltSats";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";
import { NumberInput } from "@/components/skjema/NumberInput";
import { GjennomforingDto } from "@/api/gjennomforing/utils";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  kostnadssteder: KostnadsstedOption[];
}

export function TilsagnFormPrisPerManedsverk(props: Props) {
  return (
    <TilsagnForm
      {...props}
      beregningInput={<BeregningInputSkjema prismodell={props.prismodell} />}
    />
  );
}

function BeregningInputSkjema({ prismodell }: Pick<Props, "prismodell">) {
  const { watch, getValues } = useFormContext<TilsagnRequest>();

  const periodeStart = watch("periodeStart");
  const sats = useFindAvtaltSats(prismodell, periodeStart);

  const type = getValues("beregning.type");

  return (
    <VStack gap="space-16">
      <HGrid gap="space-16" align="start" columns={2}>
        <NumberInput<TilsagnRequest>
          name="beregning.antallPlasser"
          label={tilsagnTekster.antallPlasser.label}
        />
        <TextField
          size="small"
          type="number"
          label={tilsagnTekster.sats.label(type)}
          readOnly
          value={sats?.pris.belop ?? 0}
        />
      </HGrid>
    </VStack>
  );
}
