import { Box, Heading, VStack } from "@chakra-ui/react";
import TopicOverview from "../sections/TopicOverview";
import { ApiBase } from "../core/api.tsx";

export function Tiltakshistorikk() {
  return (
    <Box>
      <Heading mb="10">tiltakshistorikk</Heading>
      <VStack spacing={8}>
        <TopicOverview base={ApiBase.TILTAKSHISTORIKK} />
      </VStack>
    </Box>
  );
}
