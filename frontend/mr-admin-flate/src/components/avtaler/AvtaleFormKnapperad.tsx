import { Button, Heading, Spacer } from "@navikt/ds-react";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";
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
      <HarSkrivetilgang ressurs="Avtale">
        <Button size="small" type="submit">
          Lagre redigert avtale
        </Button>
      </HarSkrivetilgang>
    </SkjemaKnapperad>
  );
}
