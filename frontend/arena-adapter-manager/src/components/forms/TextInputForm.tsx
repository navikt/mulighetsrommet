import { useState } from "react";
import { Button, TextField, VStack } from "@navikt/ds-react";

interface TextInputFormProps {
  label: string;
  description?: string;
  name: string;
  onSubmit: (data: Record<string, string>) => void;
  loading: boolean;
}

export function TextInputForm({ label, description, name, onSubmit, loading }: TextInputFormProps) {
  const [value, setValue] = useState("");
  const [hasError, setHasError] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (value.trim() === "") {
      setHasError(true);
      return;
    }
    setHasError(false);
    onSubmit({ [name]: value });
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack gap="4" align="start" marginBlock="6">
        <TextField
          label={label}
          description={description}
          error={hasError && "Dette feltet kan ikke være tomt"}
          value={value}
          onChange={(e) => setValue(e.target.value)}
        />
        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
