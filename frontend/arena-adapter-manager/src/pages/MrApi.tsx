import { Box, Heading, Link, VStack } from "@chakra-ui/react";
import { RunTask } from "../sections/RunTask";
import TopicOverview from "../sections/TopicOverview.tsx";
import { ApiBase } from "../core/api.tsx";

export function MrApi() {
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
          task="initial-load-gjennomforinger"
          input={{
            oneOf: [
              { $ref: "#/definitions/tiltakstyperInput" },
              { $ref: "#/definitions/idsInput" },
            ],
            definitions: {
              tiltakstyperInput: {
                type: "object",
                title: "Initial load basert på tiltakstyper",
                description:
                  "Starter en initial load av gjennomføringer filtrert basert på input fra skjemaet.",
                properties: {
                  tiltakstyper: {
                    title: "Tiltakstyper",
                    description: "For hvilke tiltakstyper skal gjennomføringer relastes på topic?",
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
                    minItems: 1,
                  },
                },
                required: ["tiltakstyper"],
              },
              idsInput: {
                type: "object",
                title: "Send ny melding basert på id",
                description:
                  "Hvis det ikke finnes gjennomdøring for gitt id blir det sendt en tombstone-melding i stedet.",
                properties: {
                  id: {
                    title: "ID til gjennomføring",
                    description: "Flere id'er kan separeres med et komma (,)",
                    type: "string",
                  },
                  bekreftelse: {
                    title:
                      "Jeg forstår at tombstone-melding vil bli sendt om det ikke finnes noen gjennomføring for gitt id",
                    type: "boolean",
                    enum: [true],
                  },
                },
                required: ["id", "bekreftelse"],
              },
            },
          }}
        />

        <RunTask
          base={ApiBase.MR_API}
          task="republish-opprett-bestilling"
          input={{
            type: "object",
            title: 'Resend "Opprett bestilling" basert på bestillingsnummer',
            properties: {
              bestillingsnummer: {
                title: "Bestillingsnummer til tilsagn",
                description: "Flere bestillingsnummere kan separeres med et komma (,)",
                type: "string",
              },
            },
            required: ["bestillingsnummer"],
          }}
        />

        <RunTask
          base={ApiBase.MR_API}
          task="republish-opprett-faktura"
          input={{
            type: "object",
            title: 'Resend "Opprett faktura" basert på fakturanummer',
            properties: {
              fakturanummer: {
                title: "Fakturanummer til delutbetaling",
                description: "Flere fakturanummer kan separeres med et komma (,)",
                type: "string",
              },
            },
            required: ["fakturanummer"],
          }}
        />

        <RunTask base={ApiBase.MR_API} task={"sync-navansatte"}>
          Synkroniserer Nav-ansatte fra relevante AD-grupper.
        </RunTask>

        <RunTask base={ApiBase.MR_API} task={"sync-utdanning"}>
          Synkroniserer data fra utdanning.no.
        </RunTask>

        <RunTask
          base={ApiBase.MR_API}
          task={"sync-arrangorer"}
          input={{
            type: "object",
            required: ["organisasjonsnummer"],
            properties: {
              organisasjonsnummer: {
                title: "Organisasjonsnummer til arrangør som skal synkroniseres med Brreg",
                description: "Flere organisasjonsnummer kan separeres med et komma (,)",
                type: "string",
              },
            },
          }}
        />

        <RunTask
          base={ApiBase.MR_API}
          task={"generate-utbetaling"}
          input={{
            type: "object",
            required: ["date"],
            properties: {
              date: {
                type: "string",
                format: "date",
                title: "Velg dato",
                description: "Velg dato for måneden det skal genereres utbetaling for",
              },
            },
          }}
        />
      </VStack>
    </Box>
  );
}
