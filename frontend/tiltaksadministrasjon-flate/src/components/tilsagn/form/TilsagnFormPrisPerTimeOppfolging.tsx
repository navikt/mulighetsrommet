import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { PrismodellDto, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { HGrid, TextField, VStack } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";
import { useFindAvtaltSats } from "@/api/avtaler/useFindAvtaltSats";
import { KostnadsstedOption } from "@/components/tilsagn/form/VelgKostnadssted";
import { NumberInput } from "@/components/skjema/NumberInput";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";
import { GjennomforingDto } from "@/api/gjennomforing/utils";

interface Props {
  gjennomforing: GjennomforingDto;
  prismodell: PrismodellDto;
  onSuccess: () => void;
  onAvbryt: () => void;
  defaultValues: TilsagnRequest;
  kostnadssteder: KostnadsstedOption[];
}

export function TilsagnFormPrisPerTimeOppfolging(props: Props) {
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
      <HGrid align="start" gap="space-16" columns={2}>
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
      <HGrid align="start" gap="space-16" columns={2}>
        <NumberInput<TilsagnRequest>
          name="beregning.antallTimerOppfolgingPerDeltaker"
          label={
            <LabelWithHelpText label={tilsagnTekster.antallTimerOppfolgingPerDeltaker.label}>
              Antall timer per deltaker til oppfølging, inkludert reisetid og rapportskriving
            </LabelWithHelpText>
          }
        />
      </HGrid>
    </VStack>
  );
}
