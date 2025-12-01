import { useMemo, useState } from "react";
import { Button, VStack, UNSAFE_Combobox } from "@navikt/ds-react";

interface TiltakstyperFormProps {
  onSubmit: (data: { tiltakstyper: string[] }) => void;
  loading: boolean;
}

const TILTAKSTYPER = [
  { value: "ARBEIDSFORBEREDENDE_TRENING", label: "Arbeidsforberedende trening" },
  { value: "ARBEIDSRETTET_REHABILITERING", label: "Arbeidsrettet rehabilitering" },
  { value: "AVKLARING", label: "Avklaring" },
  { value: "DIGITALT_OPPFOLGINGSTILTAK", label: "Digitalt oppfølgingstiltak" },
  { value: "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING", label: "Enkeltplass arbeidsmarkedsopplæring" },
  { value: "ENKELTPLASS_FAG_OG_YRKESOPPLAERING", label: "Enkeltplass fag- og yrkesopplæring" },
  { value: "GRUPPE_ARBEIDSMARKEDSOPPLAERING", label: "Gruppe arbeidsmarkedsopplæring" },
  { value: "GRUPPE_FAG_OG_YRKESOPPLAERING", label: "Gruppe fag- og yrkesopplæring" },
  { value: "HOYERE_UTDANNING", label: "Høyere utdanning" },
  { value: "JOBBKLUBB", label: "Jobbklubb" },
  { value: "OPPFOLGING", label: "Oppfølging" },
  { value: "VARIG_TILRETTELAGT_ARBEID_SKJERMET", label: "Varig tilrettelagt arbeid skjermet" },
];

export function TiltakstyperForm({ onSubmit, loading }: TiltakstyperFormProps) {
  const [value, setValue] = useState("");
  const [selectedTiltakstyper, setSelectedTiltakstyper] = useState<string[]>([]);

  const filteredOptions = useMemo(
    () => TILTAKSTYPER.filter((option) => option.label.toLowerCase().includes(value.toLowerCase())),
    [value],
  );

  const onToggleSelected = (option: string, isSelected: boolean) => {
    if (isSelected) {
      setSelectedTiltakstyper([...selectedTiltakstyper, option]);
    } else {
      setSelectedTiltakstyper(selectedTiltakstyper.filter((o) => o !== option));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ tiltakstyper: selectedTiltakstyper });
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack align="start" gap="4" marginBlock="6">
        <UNSAFE_Combobox
          label="Tiltakstyper"
          isMultiSelect
          filteredOptions={filteredOptions}
          description="For hvilke tiltakstyper skal gjennomføringer relastes på topic?"
          onToggleSelected={onToggleSelected}
          options={TILTAKSTYPER}
          onChange={setValue}
          required
          value={value}
        />

        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
