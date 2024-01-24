import { Box, Heading, VStack } from "@chakra-ui/react";
import { UpdateVirksomhet } from "../sections/UpdateVirksomhet";
import { RunTask } from "../sections/RunTask";

export function MrApiManagement() {
  return (
    <Box>
      <Heading mb="10">mr-api</Heading>
      <VStack spacing={8}>
        <UpdateVirksomhet />
        <RunTask task="generate-validation-report">
          <p>
            Genererer en rapport med alle valideringsfeil på gjennomføringer og laster rapporten opp
            til en bucket i GCP.
          </p>
          <p>
            Rapporten kan benyttes til å få en oversikt over tilstanden til gjennomføringene vi skal
            migrere.
          </p>
        </RunTask>
        <RunTask task="initial-load-mulighetsrommet-tiltaksgjennomforinger">
          Starter en initial load av alle gjennomføringer med opphav = MR_ADMIN_FLATE.
        </RunTask>
        <RunTask task="initial-load-tiltaksgjennomforinger">
          <p>Starter en initial load av alle gjennomføringer i API.</p>
          <p>
            Dette inkluderer både gjennomføringer med opphav = ARENA og opphav = MR_ADMIN_FLATE.
          </p>
        </RunTask>
        <RunTask task={"sync-navansatte"}>
          Synkoniserer NAV-ansatte fra relevante AD-grupper.
        </RunTask>
      </VStack>
    </Box>
  );
}
