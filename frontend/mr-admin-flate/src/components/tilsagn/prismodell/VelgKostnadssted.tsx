import { NavEnhet } from "@mr/api-client-v2";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useFormContext } from "react-hook-form";

interface Props {
  kostnadssteder: NavEnhet[] | undefined;
}

export function VelgKostnadssted({ kostnadssteder }: Props) {
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
