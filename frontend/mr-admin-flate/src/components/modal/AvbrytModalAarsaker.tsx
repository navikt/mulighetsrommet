import { Radio, RadioGroup, Textarea } from "@navikt/ds-react";
import { AvbrytAvtaleAarsak, AvbrytGjennomforingAarsak } from "@mr/api-client-v2";
import { AnnetEnum } from "@/api/annetEnum";

interface Props {
  aarsak?: AvbrytAvtaleAarsak | AnnetEnum | AvbrytGjennomforingAarsak;
  setAarsak: (a: string) => void;
  customAarsak?: string;
  setCustomAarsak: (a: string | undefined) => void;
  radioknapp: React.ReactNode;
  aarsakError?: string;
  customAarsakError?: string;
}
export function AvbrytModalAarsaker({
  aarsak,
  setAarsak,
  customAarsak,
  setCustomAarsak,
  radioknapp,
  aarsakError,
  customAarsakError,
}: Props) {
  return (
    <RadioGroup
      style={{ textAlign: "left" }}
      size="small"
      legend="Velg årsak"
      onChange={setAarsak}
      value={aarsak}
      error={aarsakError}
    >
      {radioknapp}
      <Radio value="annet" style={{ width: "25rem" }}>
        Annet
        {aarsak === "annet" && (
          <Textarea
            style={{ width: "25rem", minHeight: "4rem" }}
            size="small"
            placeholder="Beskrivelse"
            onChange={(e) => {
              setCustomAarsak(e.target.value);
            }}
            value={customAarsak ?? undefined}
            label={undefined}
            maxLength={100}
            error={customAarsakError}
          />
        )}
      </Radio>
    </RadioGroup>
  );
}
