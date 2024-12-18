import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useFormContext } from "react-hook-form";
import { InferredTilsagnSchemaAft } from "./OpprettTilsagnSchema";

interface Props {
  defaultKostnadssteder: string[];
}

export function VelgKostnadssted({ defaultKostnadssteder }: Props) {
  const { data: kostnadssteder } = useKostnadssted(defaultKostnadssteder);

  const { register } = useFormContext<InferredTilsagnSchemaAft>();

  return (
    <ControlledSokeSelect
      placeholder="Velg kostnadssted"
      size="small"
      label="Kostnadssted"
      {...register("kostnadssted")}
      options={
        kostnadssteder
          ?.sort((a, b) => a.navn.localeCompare(b.navn))
          .map(({ navn, enhetsnummer }) => {
            return {
              value: enhetsnummer,
              label: `${navn} - ${enhetsnummer}`,
            };
          }) ?? []
      }
    />
  );
}
