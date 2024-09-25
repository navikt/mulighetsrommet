import { Toggles } from "@mr/api-client";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { useHentUtdanninger } from "../../api/utdanning/useHentUtdanninger";
import { Alert, Select } from "@navikt/ds-react";
import { useState } from "react";

export function AvtaleUtdanningSkjema() {
  const { data: enableUtdanningskjema } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_UTDANNINGSKATEGORIER,
  );
  const { data: utdanninger, isPending } = useHentUtdanninger();
  const [programomrade, setProgramomrade] = useState<string | null>(null);

  if (!enableUtdanningskjema) {
    return null;
  }

  if (isPending) {
    return (
      <Select disabled label="Velg programområde">
        <option>Laster...</option>
      </Select>
    );
  }

  if (!utdanninger) {
    return <Alert variant="warning">Klarte ikke hente programområder og utdanninger</Alert>;
  }

  const programomrader = utdanninger.map((utdanning) => utdanning.programomrade);
  const utdanningerForProgramomrade = utdanninger
    .filter((utdanning) => utdanning.programomrade.id === programomrade)
    .map((utdanning) => utdanning.utdanninger)
    .flat();

  return (
    <>
      <Select
        size="small"
        label="Velg programområde"
        onChange={(e) => setProgramomrade(e.currentTarget.value)}
      >
        <option value={undefined}>Velg programområde</option>
        {programomrader.map((programomrade) => (
          <option value={programomrade.id} key={programomrade.id}>
            {programomrade.navn}
          </option>
        ))}
      </Select>
      {programomrade && (
        <Select label="Velg sluttkompetanse" size="small">
          {utdanningerForProgramomrade.map((utdanning) => (
            <option value={utdanning.id} key={utdanning.navn}>
              {utdanning.navn}
            </option>
          ))}
        </Select>
      )}
    </>
  );
}
