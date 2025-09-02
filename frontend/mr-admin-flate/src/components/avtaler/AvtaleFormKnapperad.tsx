import { Button, Spacer } from "@navikt/ds-react";
import { HarTilgang } from "@/components/auth/HarTilgang";
import { ValideringsfeilOppsummering } from "../skjema/ValideringsfeilOppsummering";
import { SkjemaKnapperad } from "@/components/skjema/SkjemaKnapperad";
import { useNavigate } from "react-router";
import { Rolle } from "@mr/api-client-v2";

export function AvtaleFormKnapperad() {
  const navigate = useNavigate();
  return (
    <SkjemaKnapperad>
      <Spacer />
      <ValideringsfeilOppsummering />
      <Button size="small" onClick={() => navigate(-1)} variant="tertiary" type="button">
        Avbryt
      </Button>
      <HarTilgang rolle={Rolle.AVTALER_SKRIV}>
        <Button size="small" type="submit">
          Lagre redigert avtale
        </Button>
      </HarTilgang>
    </SkjemaKnapperad>
  );
}
