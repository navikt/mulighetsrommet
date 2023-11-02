import { Box, Heading, VStack } from "@chakra-ui/react";
import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";
import ReplayEvent from "../sections/ReplayEvent";
import DeleteEvents from "../sections/DeleteEvents";

export function MrArenaAdapterManagement() {
  return (
    <Box>
      <Heading mb="10">mr-arena-adapter</Heading>
      <VStack spacing={8}>
        <TopicOverview />
        <ReplayEvents />
        <ReplayEvent />
        <DeleteEvents />
      </VStack>
    </Box>
  );
}
