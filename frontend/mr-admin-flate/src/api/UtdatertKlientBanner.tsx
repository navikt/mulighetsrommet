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
    <Box className="bg-orange-100" borderColor="border-warning" padding="4">
      <HStack align="center" justify="space-between" gap="2">
        <p className="text-m text-red-700">
          Appen er utdatert. Dette kan medføre feil. Last siden på nytt hvis mulig
        </p>

        <HStack gap="2">
          <Button
            onClick={() => window.location.reload()}
            className="px-3 py-1 rounded bg-orange-200 text-red-700 hover:bg-orange-300 transition"
          >
            Relast siden
          </Button>

          <button
            onClick={() => {
              setIgnored(true);
              setVisible(false);
            }}
            className="px-3 py-1 rounded bg-gray-200 text-gray-700 text-sm hover:bg-gray-300 transition"
          >
            Ignorer
          </button>
        </HStack>
      </HStack>
    </Box>
  );
}
