import { Box, Heading, Link, VStack } from "@chakra-ui/react";
import { UpdateVirksomhet } from "../sections/UpdateVirksomhet";
import { RunTask } from "../sections/RunTask";
import TopicOverview from "../sections/TopicOverview.tsx";
import { ApiBase } from "../core/api.tsx";

export function MrApiManagement() {
  return (
    <Box>
      <Heading mb="10">mr-api</Heading>
      <VStack spacing={8}>
        <TopicOverview base={ApiBase.MR_API} />

        <UpdateVirksomhet />

        <RunTask base={ApiBase.MR_API} task="generate-validation-report">
          <p>
            Genererer en rapport med alle valideringsfeil på gjennomføringer og laster rapporten opp
            til en
            <Link href="https://console.cloud.google.com/storage/browser">bucket i GCP.</Link>
          </p>
          <p>
            Rapporten kan benyttes til å få en oversikt over tilstanden til gjennomføringene vi skal
            migrere.
          </p>
        </RunTask>

        <RunTask base={ApiBase.MR_API} task="initial-load-mulighetsrommet-tiltaksgjennomforinger">
          Starter en initial load av alle gjennomføringer med opphav = MR_ADMIN_FLATE.
        </RunTask>

        <RunTask base={ApiBase.MR_API} task="initial-load-tiltaksgjennomforinger">
          <p>Starter en initial load av alle gjennomføringer i API.</p>
          <p>
            Dette inkluderer både gjennomføringer med opphav = ARENA og opphav = MR_ADMIN_FLATE.
          </p>
        </RunTask>

        <RunTask base={ApiBase.MR_API} task={"sync-navansatte"}>
          Synkoniserer NAV-ansatte fra relevante AD-grupper.
        </RunTask>
      </VStack>
    </Box>
  );
}
