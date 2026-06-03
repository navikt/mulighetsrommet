import { Box, Button, HStack } from "@navikt/ds-react";
import { useEffect, useState } from "react";

export function UtdatertKlientBanner() {
  const [visible, setVisible] = useState(false);
  const [ignored, setIgnored] = useState(false);

  useEffect(() => {
    function handler() {
      setVisible(true);
    }

    window.addEventListener("openapi-version-mismatch", handler);
    return () => window.removeEventListener("openapi-version-mismatch", handler);
  }, []);

  if (!visible || ignored) return null;

  return (
    <Box className="bg-ax-warning-200" borderColor="warning" padding="space-16">
      <HStack align="center" justify="space-between" gap="space-8">
        <p className="text-m text-ax-danger-800">
          Appen er utdatert. Dette kan medføre feil. Last siden på nytt hvis mulig
        </p>

        <HStack gap="space-8">
          <Button
            onClick={() => window.location.reload()}
            className="px-3 py-1 rounded bg-ax-warning-300 text-ax-danger-800 hover:bg-ax-warning-400 transition"
          >
            Relast siden
          </Button>

          <button
            onClick={() => {
              setIgnored(true);
              setVisible(false);
            }}
            className="px-3 py-1 rounded bg-ax-neutral-300 text-ax-neutral-800 text-sm hover:bg-ax-neutral-400 transition"
          >
            Ignorer
          </button>
        </HStack>
      </HStack>
    </Box>
  );
}
