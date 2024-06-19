import { Box, Heading, Link, VStack } from "@chakra-ui/react";
import { RunTask } from "../sections/RunTask";
import TopicOverview from "../sections/TopicOverview.tsx";
import { ApiBase } from "../core/api.tsx";

export function MrApiManagement() {
  return (
    <Box>
      <Heading mb="10">mr-api</Heading>
      <VStack spacing={8}>
        <TopicOverview base={ApiBase.MR_API} />

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

        <RunTask base={ApiBase.MR_API} task="initial-load-tiltakstyper">
          Starter en initial load av alle relevante tiltakstyper.
        </RunTask>

        <RunTask
          base={ApiBase.MR_API}
          task="initial-load-tiltaksgjennomforinger"
          input={{
            type: "object",
            description:
              "Starter en initial load av gjennomføringer filtrert basert på input fra skjemaet.",
            properties: {
              opphav: {
                title: "Opphav",
                description:
                  "For hvilket opphav skal gjennomføringer relastes på topic? Hvis feltet er tomt vil gjennomføringer relastes uavhengig av opphav.",
                type: "string",
                enum: ["MR_ADMIN_FLATE", "ARENA"],
              },
              tiltakstyper: {
                title: "Tiltakstyper",
                description:
                  "For hvilke tiltakstyper skal gjennomføringer relastes på topic? Hvis ingen er valgt vil gjennomføringer relastes for alle tiltakstyper.",
                type: "array",
                items: {
                  type: "string",
                  enum: [
                    "AVKLARING",
                    "OPPFOLGING",
                    "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
                    "JOBBKLUBB",
                    "DIGITALT_OPPFOLGINGSTILTAK",
                    "ARBEIDSFORBEREDENDE_TRENING",
                    "GRUPPE_FAG_OG_YRKESOPPLAERING",
                    "ARBEIDSRETTET_REHABILITERING",
                    "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
                  ],
                },
                uniqueItems: true,
              },
            },
          }}
        />

        <RunTask base={ApiBase.MR_API} task={"sync-navansatte"}>
          Synkroniserer NAV-ansatte fra relevante AD-grupper.
        </RunTask>
        <RunTask base={ApiBase.MR_API} task={"sync-utdanning"}>
          Synkroniserer data fra utdanning.no.
        </RunTask>
      </VStack>
    </Box>
  );
}
