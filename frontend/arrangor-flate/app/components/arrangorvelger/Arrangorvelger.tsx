import { BodyShort, Box, Button, HStack, VStack } from "@navikt/ds-react";
import { ArrangorflateArrangor } from "api-client";
import { PencilIcon } from "@navikt/aksel-icons";
import { useParams } from "react-router";

interface Props {
  arrangorer: ArrangorflateArrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const { orgnr } = useParams();
  const arrangor = arrangorer.find((a) => a.organisasjonsnummer === orgnr);

  if (!arrangor) {
    return null;
  }

  return (
    <Box padding="2" borderRadius="medium" borderWidth="1">
      <VStack gap="2" align="center" width="20rem">
        <BodyShort size="small" weight="semibold">
          {arrangor.navn}
        </BodyShort>
        <HStack gap="4" justify="space-between" align="center" width="100%">
          <BodyShort size="small" weight="semibold">
            {orgnr}
          </BodyShort>
          <Button
            variant="secondary-neutral"
            size="small"
            as="a"
            href="/"
            iconPosition="right"
            icon={<PencilIcon aria-hidden />}
          >
            Endre
          </Button>
        </HStack>
      </VStack>
    </Box>
  );
}
