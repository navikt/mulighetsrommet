import { FormCombobox } from "@/components/skjema/FormCombobox";
import { tilsagnTekster } from "../TilsagnTekster";

interface Props {
  kostnadssteder: KostnadsstedOption[];
}

export interface KostnadsstedOption {
  enhetsnummer: string;
  navn: string;
}

export function VelgKostnadssted({ kostnadssteder }: Props) {
  const options = kostnadssteder
    .sort((a, b) => a.navn.localeCompare(b.navn))
    .map(({ navn, enhetsnummer }) => {
      return {
        value: enhetsnummer,
        label: `${navn} - ${enhetsnummer}`,
      };
    });

  return (
    <FormCombobox<{ kostnadssted: string }>
      placeholder="Velg kostnadssted"
      size="small"
      label={tilsagnTekster.kostnadssted.label}
      name="kostnadssted"
      options={options}
    />
  );
}
