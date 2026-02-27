import { RunTask } from "../sections/RunTask";
import TopicOverview from "../sections/TopicOverview.tsx";
import { ApiBase } from "../core/api.tsx";
import { BodyShort, Link } from "@navikt/ds-react";
import { TiltakstyperOrIdsForm } from "../components/forms/TiltakstyperOrIdsForm";
import { TextInputForm } from "../components/forms/TextInputForm";
import { DatePickerForm } from "../components/forms/DatePickerForm";
import FailedScheduledTasksOverview from "../sections/FailedScheduledTasksOverview.tsx";
import FailedKafkaConsumerRecordsOverview from "../sections/FailedKafkaConsumerRecordsOverview.tsx";

export function MrApi() {
  return (
    <>
      <TopicOverview base={ApiBase.MR_API} />
      <FailedKafkaConsumerRecordsOverview base={ApiBase.MR_API} />
      <FailedScheduledTasksOverview base={ApiBase.MR_API} />

      <RunTask base={ApiBase.MR_API} task="initial-load-tiltakstyper">
        <BodyShort>Starter en initial load av alle relevante tiltakstyper.</BodyShort>
      </RunTask>

      <RunTask
        base={ApiBase.MR_API}
        task="initial-load-gjennomforinger"
        form={(props) => <TiltakstyperOrIdsForm {...props} />}
      />

      <RunTask
        base={ApiBase.MR_API}
        task="republish-opprett-bestilling"
        form={(props) => (
          <TextInputForm
            {...props}
            label="Bestillingsnummer til tilsagn"
            description="Flere bestillingsnummere kan separeres med et komma (,)"
            name="bestillingsnummer"
          />
        )}
      />

      <RunTask
        base={ApiBase.MR_API}
        task="republish-opprett-faktura"
        form={(props) => (
          <TextInputForm
            {...props}
            label="Fakturanummer til delutbetaling"
            description="Flere fakturanummer kan separeres med et komma (,)"
            name="fakturanummer"
          />
        )}
      />

      <RunTask base={ApiBase.MR_API} task={"sync-navansatte"}>
        <BodyShort>Synkroniserer Nav-ansatte fra relevante AD-grupper.</BodyShort>
      </RunTask>

      <RunTask base={ApiBase.MR_API} task={"sync-utdanning"}>
        <BodyShort>Synkroniserer data fra utdanning.no.</BodyShort>
      </RunTask>

      <RunTask
        base={ApiBase.MR_API}
        task={"sync-arrangorer"}
        form={(props) => (
          <TextInputForm
            {...props}
            label="Organisasjonsnummer til arrangør som skal synkroniseres med Brreg"
            description="Flere organisasjonsnummer kan separeres med et komma (,)"
            name="organisasjonsnummer"
          />
        )}
      />

      <RunTask
        base={ApiBase.MR_API}
        task={"generate-utbetaling"}
        form={(props) => (
          <DatePickerForm
            {...props}
            label="Velg dato"
            description="Velg dato for måneden det skal genereres utbetaling for"
          />
        )}
      />

      <RunTask
        base={ApiBase.MR_API}
        task={"beregn-utbetaling"}
        form={(props) => (
          <DatePickerForm
            {...props}
            label="Velg dato"
            description="Velg dato for måneden det skal beregnes utbetalinger for"
          />
        )}
      >
        <BodyShort>
          Beregner utbetalinger for gitt måned og genererer en rapport som inneholder forskjeller
          mellom utbetalinger i som allerede er behandlet i Tiltaksadministrasjon sammenlignet med
          ny beregning av utbetalingen.
        </BodyShort>
        <BodyShort>
          Rapporten blir tilgjengelig i en{" "}
          <Link href="https://console.cloud.google.com/storage/browser">GCP bucket</Link> når jobben
          har kjørt ferdig.
        </BodyShort>
      </RunTask>

      <RunTask
        base={ApiBase.MR_API}
        task={"journalfor-tilsagnsbrev"}
        form={(props) => <TextInputForm {...props} label="Tilsangs id" name="tilsagnId" />}
      >
        <BodyShort>Lager et tilsagnsbrev</BodyShort>
      </RunTask>

      <RunTask
        base={ApiBase.MR_API}
        task={"distribuer-tilsagnsbrev"}
        form={(props) => (
          <TextInputForm
            {...props}
            label="Tilsangs id med eksisterende journalført tilsagnsbrev"
            name="tilsagnId"
          />
        )}
      >
        <BodyShort>
          Distribuerer et tilsagnsbrev for gitt tilsagn id. Tryner om ikke journalpostId eksisterer
          for tilsagnet (kjør journalføring av tilsagnsbrev først)
        </BodyShort>
      </RunTask>
    </>
  );
}
