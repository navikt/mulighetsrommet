import { NavEnhetDto } from "@mr/api-client-v2";
import { ControlledSokeSelect } from "@mr/frontend-common";
import { useFormContext } from "react-hook-form";
import { tilsagnTekster } from "../TilsagnTekster";

interface Props {
  kostnadssteder: NavEnhetDto[] | undefined;
}

export function VelgKostnadssted({ kostnadssteder }: Props) {
  const { register } = useFormContext<{ kostnadssted: string }>();

  return (
    <ControlledSokeSelect
      placeholder="Velg kostnadssted"
      size="small"
      label={tilsagnTekster.kostnadssted.label}
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
