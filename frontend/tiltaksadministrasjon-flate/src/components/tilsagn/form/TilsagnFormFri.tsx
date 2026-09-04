import { TilsagnForm } from "@/components/tilsagn/form/TilsagnForm";
import { PrismodellDto, TilsagnRequest } from "@tiltaksadministrasjon/api-client";
import { HGrid, VStack } from "@navikt/ds-react";
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

export function TilsagnFormFri(props: Props) {
  return <TilsagnForm {...props} beregningInput={<BeregningInputSkjema />} />;
}

function BeregningInputSkjema() {
  return (
    <VStack gap="space-16">
      <HGrid gap="space-16" align="start" columns={2}>
        <NumberInput<TilsagnRequest> name="beregning.pris.belop" label="Tilsagnsbeløp" />
      </HGrid>
    </VStack>
  );
}
