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
  { value: "TILRETTELAGT_ARBEID_ORDINAER", label: "Varig tilrettelagt arbeid ordinær" },
  { value: "ARBEIDSMARKEDSOPPLAERING", label: "Arbeidsmarkedsopplæring (AMO)" },
  {
    value: "NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV",
    label: "Norskopplæring, grunnleggende ferdigheter og FOV",
  },
  { value: "STUDIESPESIALISERING", label: "Studiespesialisering" },
  { value: "FAG_OG_YRKESOPPLAERING", label: "Fag og yrkesopplæring" },
  { value: "HOYERE_YRKESFAGLIG_UTDANNING", label: "Høyere yrkesfaglig opplæring" },
];

export function TiltakstyperForm({ onSubmit, loading }: TiltakstyperFormProps) {
  const [value, setValue] = useState("");
  const [selectedTiltakstyper, setSelectedTiltakstyper] = useState<string[]>([]);
  const [hasError, setHasError] = useState(false);

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
    if (selectedTiltakstyper.length === 0) {
      setHasError(true);
      return;
    }
    setHasError(false);
    onSubmit({ tiltakstyper: selectedTiltakstyper });
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack align="start" gap="space-16" marginBlock="space-24">
        <UNSAFE_Combobox
          label="Tiltakstyper"
          isMultiSelect
          filteredOptions={filteredOptions}
          description="For hvilke tiltakstyper skal gjennomføringer relastes på topic?"
          onToggleSelected={onToggleSelected}
          options={TILTAKSTYPER}
          onChange={setValue}
          error={hasError && "Du må velge minst én tiltakstype"}
          value={value}
        />

        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
