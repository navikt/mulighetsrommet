import { Box, Heading, VStack } from "@chakra-ui/react";
import ReplayEvents from "../sections/ReplayEvents";
import TopicOverview from "../sections/TopicOverview";
import ReplayEvent from "../sections/ReplayEvent";
import DeleteEvents from "../sections/DeleteEvents";
import UpdateVirksomhet from "../sections/UpdateVirksomhet";

function Management() {
  return (
    <Box>
      <Heading mb="10">Management</Heading>
      <VStack spacing={8}>
        <TopicOverview />
        <ReplayEvents />
        <ReplayEvent />
        <DeleteEvents />
        <UpdateVirksomhet />
      </VStack>
    </Box>
  );
}

export default Management;
