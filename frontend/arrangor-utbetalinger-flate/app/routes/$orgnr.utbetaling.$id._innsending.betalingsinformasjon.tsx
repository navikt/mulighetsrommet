import { Heading, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { MetaFunction, useLocation } from "react-router";
import { BetalingsinformasjonInput } from "~/components/utbetaling/BetalingsinformasjonInput";
import { StepFooter } from "~/components/utbetaling/StepFooter";
import { useArrangorflateUtbetaling } from "~/hooks/useArrangorflateUtbetaling";
import { useSyncKontonummer } from "~/hooks/useSyncKontonummer";
import { useUtbetalingWizard } from "~/hooks/useUtbetalingWizard";
import { useIdFromUrl } from "~/utils/navigation";

export const meta: MetaFunction = () => {
  return [
    { title: "Steg 3 av 4: Betalingsinformasjon - Godkjenn innsending" },
    {
      name: "description",
      content: "Kontonummer og KID-nummer for utbetalingen",
    },
  ];
};

export default function Betalingsinformasjon() {
  const id = useIdFromUrl();
  const { kid: kidFromState } = useLocation().state || {};

  const { data: utbetaling } = useArrangorflateUtbetaling(id);
  const syncKontonummer = useSyncKontonummer(id);
  const wizard = useUtbetalingWizard(utbetaling);

  const [kid, setKid] = useState(kidFromState ?? utbetaling.betalingsinformasjon?.kid ?? "");
  const [kontonummerError, setKontonummerError] = useState<string>();

  const validateAndContinue = () => {
    if (!utbetaling.betalingsinformasjon?.kontonummer) {
      setKontonummerError("Fant ikke kontonummer");
      return;
    }
    wizard.goToNext({ kid });
  };

  return (
    <VStack gap="space-16">
      <Heading level="2" spacing size="large">
        Betalingsinformasjon
      </Heading>
      <BetalingsinformasjonInput
        kontonummer={utbetaling.betalingsinformasjon?.kontonummer}
        kontonummerError={kontonummerError}
        onSyncKontonummer={() => syncKontonummer.mutate()}
        kid={kid}
        onKidChange={setKid}
      />
      <StepFooter wizard={wizard} primaryAction={{ onClick: validateAndContinue }} />
    </VStack>
  );
}
