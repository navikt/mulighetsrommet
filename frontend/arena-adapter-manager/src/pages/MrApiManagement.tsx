import { Box, Heading, VStack } from "@chakra-ui/react";
import { UpdateVirksomhet } from "../sections/UpdateVirksomhet";
import { RunTask } from "../sections/RunTask";

export function MrApiManagement() {
  return (
    <Box>
      <Heading mb="10">mr-api</Heading>
      <VStack spacing={8}>
        <UpdateVirksomhet />
        <RunTask task={"generate-validation-report"} />
        <RunTask task={"initial-load-tiltaksgjennomforinger"} />
      </VStack>
    </Box>
  );
}
