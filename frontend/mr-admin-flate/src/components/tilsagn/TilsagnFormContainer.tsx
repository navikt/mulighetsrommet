import { TilsagnFormForhandsgodkjent } from "@/components/tilsagn/prismodell/TilsagnFormForhandsgodkjent";
import { TilsagnFormFri } from "@/components/tilsagn/prismodell/TilsagnFormFri";
import {
  AvtaleDto,
  Avtaletype,
  TilsagnType,
  GjennomforingDto,
  Prismodell,
} from "@mr/api-client-v2";
import { useNavigate } from "react-router";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { DeepPartial } from "react-hook-form";

interface Props {
  avtale: AvtaleDto;
  gjennomforing: GjennomforingDto;
  defaults: DeepPartial<InferredTilsagn>;
}

export function TilsagnFormContainer({ avtale, gjennomforing, defaults }: Props) {
  const navigate = useNavigate();

  function navigerTilTilsagn() {
    navigate(`/gjennomforinger/${gjennomforing.id}/tilsagn`);
  }

  const regionerForKostnadssteder = getRegionerForKostnadssteder(gjennomforing, defaults.type);

  const props = {
    regioner: regionerForKostnadssteder,
    gjennomforing: gjennomforing,
    onSuccess: navigerTilTilsagn,
    onAvbryt: navigerTilTilsagn,
  };

  function prismodell(avtaleType: Avtaletype, defaultPrismodell?: Prismodell): Prismodell {
    if (defaultPrismodell) {
      return defaultPrismodell;
    }
    return avtaleType === Avtaletype.FORHAANDSGODKJENT
      ? Prismodell.FORHANDSGODKJENT
      : Prismodell.FRI;
  }

  switch (prismodell(avtale.avtaletype, defaults.beregning?.type as Prismodell)) {
    case Prismodell.FORHANDSGODKJENT:
      return (
        <TilsagnFormForhandsgodkjent
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "FORHANDSGODKJENT" },
          }}
          {...props}
        />
      );
    default:
      return (
        <TilsagnFormFri
          defaultValues={{
            ...defaults,
            beregning: { ...defaults.beregning, type: "FRI" },
          }}
          {...props}
        />
      );
  }
}

function getRegionerForKostnadssteder(gjennomforing: GjennomforingDto, type?: TilsagnType) {
  return type === TilsagnType.TILSAGN
    ? gjennomforing.kontorstruktur.map((struktur) => struktur.region.enhetsnummer)
    : [];
}
