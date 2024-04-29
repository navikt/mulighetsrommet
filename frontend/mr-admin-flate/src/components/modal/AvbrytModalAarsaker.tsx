import { Radio, RadioGroup, Textarea } from "@navikt/ds-react";

interface Props {
  aarsak: string | null;
  setAarsak: (a: string) => void;
  customAarsak: string | null;
  setCustomAarsak: (a: string | null) => void;
  radioknapp: React.ReactNode;
}
export function AvbrytModalAarsaker({
  aarsak,
  setAarsak,
  customAarsak,
  setCustomAarsak,
  radioknapp,
}: Props) {
  return (
    <RadioGroup size="small" legend="Velg årsak" onChange={setAarsak} value={aarsak}>
      {radioknapp}
      <Radio value="annet">
        Annet
        {aarsak === "annet" && (
          <Textarea
            style={{ width: "22rem", minHeight: "4rem" }}
            size="small"
            placeholder="Beskrivelse"
            onChange={(e) => setCustomAarsak(e.target.value)}
            value={customAarsak ?? undefined}
            label={undefined}
            required={aarsak === "annet"}
          />
        )}
      </Radio>
    </RadioGroup>
  );
}
