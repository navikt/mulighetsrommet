import { Box, Heading, VStack } from "@chakra-ui/react";
import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";
import ReplayEvent from "../sections/ReplayEvent";

function Management() {
  return (
    <Box>
      <Heading mb="10">Management</Heading>
      <VStack spacing={8}>
        <TopicOverview />
        <ReplayEvents />
        <ReplayEvent />
        <div>Test</div>
      </VStack>
    </Box>
  );
}

export default Management;
