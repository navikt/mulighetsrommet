import { Button, Heading, Spacer } from "@navikt/ds-react";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";
import { useNavigate } from "react-router";

export function AvtaleFormKnapperad() {
  const navigate = useNavigate();
  return (
    <SkjemaKnapperad>
      <Heading size="medium" level="2">
        Rediger avtale
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
