import { Button, Heading, Spacer } from "@navikt/ds-react";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";
import { useNavigate } from "react-router";

export function AvtaleFormKnapperad({ heading }: { heading?: string }) {
  const navigate = useNavigate();
  return (
    <SkjemaKnapperad>
      <Heading level="2" size="medium">
        {heading}
      </Heading>
      <Spacer />
      <ValideringsfeilOppsummering />
      <Button size="small" onClick={() => navigate(-1)} variant="tertiary" type="button">
        Avbryt
      </Button>
      <Button size="small" type="submit">
        Lagre redigert avtale
      </Button>
    </SkjemaKnapperad>
  );
}
