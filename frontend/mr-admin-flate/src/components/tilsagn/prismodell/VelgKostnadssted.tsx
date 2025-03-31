import { useKostnadssted } from "@/api/enhet/useKostnadssted";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useFormContext } from "react-hook-form";

interface Props {
  regioner: string[];
}

export function VelgKostnadssted({ regioner }: Props) {
  const { data: kostnadssteder } = useKostnadssted(regioner);
  const { register } = useFormContext<{ kostnadssted: string }>();

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
