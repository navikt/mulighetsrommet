import { NavEnhetDto } from "@tiltaksadministrasjon/api-client";
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
      className="max-w-md"
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
