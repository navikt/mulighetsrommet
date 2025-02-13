import { Box, Heading, VStack } from "@chakra-ui/react";
import TopicOverview from "../sections/TopicOverview";
import { ApiBase } from "../core/api.tsx";

export function Tiltaksokonomi() {
  return (
    <Box>
      <Heading mb="10">tiltaksøkonomi</Heading>
      <VStack spacing={8}>
        <TopicOverview base={ApiBase.TILTAKSOKONOMI} />
      </VStack>
    </Box>
  );
}
