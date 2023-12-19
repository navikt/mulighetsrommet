import { Button, HStack } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { RedaksjoneltInnholdForm } from "../redaksjonelt-innhold/RedaksjoneltInnholdForm";
import { useFormContext } from "react-hook-form";
import { InferredTiltaksgjennomforingSchema } from "./TiltaksgjennomforingSchema";
import { useState } from "react";
import { RedaksjoneltInnholdContainer } from "../redaksjonelt-innhold/RedaksjoneltInnholdContainer";

interface Props {
  avtale: Avtale;
}

export function TiltakgjennomforingRedaksjoneltInnholdForm({ avtale }: Props) {
  const [key, setKey] = useState(0);
  const gjenopprettInnhold = useGjenopprettInnhold(avtale);

  return (
    <>
      <RedaksjoneltInnholdContainer>
        <HStack justify="end">
          <Button
            variant="tertiary"
            type="button"
            onClick={() => {
              gjenopprettInnhold();

              // Ved å endre `key` så tvinger vi en update av den underliggende Slate-komponenten slik at
              // innhold i komponenten blir resatt til å reflektere den nye tilstanden i skjemaet
              setKey(key + 1);
            }}
          >
            Gjenopprett til redaksjonelt innhold fra avtale
          </Button>
        </HStack>
      </RedaksjoneltInnholdContainer>

      <RedaksjoneltInnholdForm
        key={`redaksjonelt-innhold-${key}`}
        tiltakstype={avtale.tiltakstype}
      />
    </>
  );
}

function useGjenopprettInnhold(avtale: Avtale) {
  const { setValue } = useFormContext<InferredTiltaksgjennomforingSchema>();

  function gjenopprettInnhold() {
    setValue("beskrivelse", avtale.beskrivelse ?? null);
    setValue("faneinnhold", avtale.faneinnhold ?? null);
  }

  return gjenopprettInnhold;
}
