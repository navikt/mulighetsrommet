import { Radio, RadioGroup, Textarea } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import { ApiError } from "mulighetsrommet-api-client";

interface Props {
  aarsak: string | null;
  setAarsak: (a: string) => void;
  customAarsak: string | null;
  setCustomAarsak: (a: string | null) => void;
  radioknapp: React.ReactNode;
  mutation: UseMutationResult<unknown, ApiError, { id: string; aarsak: string | null }, unknown>;
}
export function AvbrytModalAarsaker({
  aarsak,
  setAarsak,
  customAarsak,
  setCustomAarsak,
  radioknapp,
  mutation,
}: Props) {
  return (
    <RadioGroup size="small" legend="Velg årsak" onChange={setAarsak} value={aarsak} required>
      {radioknapp}
      <Radio value="annet">
        Annet
        {aarsak === "annet" && (
          <Textarea
            style={{ width: "22rem", minHeight: "4rem" }}
            size="small"
            placeholder="Beskrivelse"
            onChange={(e) => {
              setCustomAarsak(e.target.value);
              mutation.reset();
            }}
            value={customAarsak ?? undefined}
            label={undefined}
            required={aarsak === "annet"}
            maxLength={100}
          />
        )}
      </Radio>
    </RadioGroup>
  );
}
