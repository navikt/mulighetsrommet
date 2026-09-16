import { FormCombobox } from "@/components/skjema/FormCombobox";
import { tilsagnTekster } from "../TilsagnTekster";
import { FieldPath, FieldValues } from "react-hook-form";

interface Props<T extends FieldValues> {
  kostnadssteder: KostnadsstedOption[];
  name: FieldPath<T>;
}

export interface KostnadsstedOption {
  enhetsnummer: string;
  navn: string;
}

export function VelgKostnadssted<T extends FieldValues>({ kostnadssteder, name }: Props<T>) {
  const options = kostnadssteder
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map(({ navn, enhetsnummer }) => {
      return {
        value: enhetsnummer,
        label: `${navn} - ${enhetsnummer}`,
      };
    });

  return (
    <FormCombobox<T>
      placeholder="Velg kostnadssted"
      size="small"
      label={tilsagnTekster.kostnadssted.label}
      name={name}
      options={options}
    />
  );
}
