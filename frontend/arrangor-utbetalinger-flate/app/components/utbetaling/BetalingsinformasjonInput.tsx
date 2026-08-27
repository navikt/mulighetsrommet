import { TextField, VStack } from "@navikt/ds-react";
import { KontonummerInput } from "~/components/utbetaling/KontonummerInput";

interface Props {
  kontonummer?: string | null;
  kontonummerError?: string;
  onSyncKontonummer: () => void;
  kid: string;
  onKidChange: (kid: string) => void;
  kidError?: string;
}

export function BetalingsinformasjonInput({
  kontonummer,
  kontonummerError,
  onSyncKontonummer,
  kid,
  onKidChange,
  kidError,
}: Props) {
  return (
    <VStack gap="space-16">
      <KontonummerInput
        kontonummer={kontonummer}
        error={kontonummerError}
        onClick={onSyncKontonummer}
      />
      <TextField
        label="KID-nummer for utbetaling (valgfritt)"
        size="small"
        name="kid"
        htmlSize={35}
        error={kidError}
        value={kid}
        onChange={(e) => onKidChange(e.target.value)}
        maxLength={25}
        id="kid"
      />
    </VStack>
  );
}
