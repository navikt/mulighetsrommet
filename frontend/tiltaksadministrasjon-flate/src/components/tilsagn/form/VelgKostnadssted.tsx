import { FormCombobox } from "@/components/skjema/FormCombobox";
import { tilsagnTekster } from "../TilsagnTekster";
import { FieldPath, FieldValues } from "react-hook-form";
import { ReactNode } from "react";
import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";

interface Props<T extends FieldValues> {
  name: FieldPath<T>;
  label?: ReactNode;
}

export interface KostnadsstedOption {
  enhetsnummer: string;
  navn: string;
}

export function VelgKostnadssted<T extends FieldValues>({
  name,
  label = tilsagnTekster.kostnadssted.label,
}: Props<T>) {
  const { data: kostnadssteder } = useKostnadssteder();

  const options = kostnadssteder
    .flatMap((r: RegionKostnadssteder) => r.kostnadssteder)
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map(({ navn, enhetsnummer }) => {
      return {
        value: enhetsnummer,
        label: `${navn} - ${enhetsnummer}`,
      };
    });

  return (
    <FormCombobox<T>
      placeholder="-- Velg kostnadssted --"
      size="small"
      label={label}
      name={name}
      options={options}
    />
  );
}
