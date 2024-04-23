import { Radio, RadioGroup, Textarea } from "@navikt/ds-react";

interface Props {
  aarsak: string | null;
  setAarsak: (a: string) => void;
  customAarsak: string | null;
  setCustomAarsak: (a: string | null) => void;
  aarsakToString: React.ReactNode;
}
export function AvbrytModalAarsaker({
  aarsak,
  setAarsak,
  customAarsak,
  setCustomAarsak,
  aarsakToString,
}: Props) {
  return (
    <RadioGroup size="small" legend="Velg årsak" onChange={setAarsak} value={aarsak}>
      {aarsakToString}
      <Radio value="annet">
        Annet
        {aarsak === "annet" && (
          <Textarea
            size="small"
            placeholder="Beskrivelse"
            onChange={(e) => setCustomAarsak(e.target.value)}
            value={customAarsak ?? undefined}
            label={undefined}
          />
        )}
      </Radio>
    </RadioGroup>
  );
}
