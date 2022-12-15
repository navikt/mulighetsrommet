import { Box, Heading, VStack } from "@chakra-ui/react";
import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";

function Management() {
  return (
    <Box>
      <Heading mb="10">Management</Heading>
      <VStack spacing={8}>
        <TopicOverview />
        <ReplayEvents />
      </VStack>
    </Box>
  );
}

export default Management;
