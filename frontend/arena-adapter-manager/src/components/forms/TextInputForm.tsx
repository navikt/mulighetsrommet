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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ [name]: value });
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack gap="4" align="start" marginBlock="6">
        <TextField
          label={label}
          description={description}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          required
        />
        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
