import { useState } from "react";
import { Button, TextField, VStack } from "@navikt/ds-react";

interface TextInputFormProps {
  onSubmit: (data: Record<string, string>) => void;
  loading: boolean;
}

export function JournalforTilsagnsbrevForm({ onSubmit, loading }: TextInputFormProps) {
  const [tilsagnId, setTilsagnId] = useState("");
  const [deltakerId, setDeltakerId] = useState("");
  const [hasError, setHasError] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (tilsagnId.trim() === "" || deltakerId.trim() === "") {
      setHasError(true);
      return;
    }
    setHasError(false);
    onSubmit({ tilsagnId: tilsagnId, deltakerId: deltakerId });
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack gap="4" align="start" marginBlock="6">
        <TextField
          label="Tilsagn id"
          error={hasError && "Dette feltet kan ikke være tomt"}
          value={tilsagnId}
          onChange={(e) => setTilsagnId(e.target.value)}
        />
        <TextField
          label="Deltaker id"
          description="Gjerne under sammme gjennomføring som tilsagnet gjelder"
          error={hasError && "Dette feltet kan ikke være tomt"}
          value={deltakerId}
          onChange={(e) => setDeltakerId(e.target.value)}
        />
        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
