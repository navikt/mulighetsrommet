import { Box, Heading, VStack } from "@chakra-ui/react";
import TopicOverview from "../sections/TopicOverview";

function Management() {
  return (
    <Box>
      <Heading mb="10">Management</Heading>
      <VStack spacing={8}>
        <TopicOverview />
      </VStack>
    </Box>
  );
}

export default Management;
