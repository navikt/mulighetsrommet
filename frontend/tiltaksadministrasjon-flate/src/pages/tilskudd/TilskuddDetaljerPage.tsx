import { useTilskudd } from "@/api/tilskudd/useTilskuddOrError";
import { TilskuddLayout } from "@/components/tilskudd/TilskuddLayout";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { BodyShort, ExpansionCard, Heading, VStack } from "@navikt/ds-react";
import { TilskuddVedtak } from "@tiltaksadministrasjon/api-client";
import { tilskuddMottakerToString } from "@/utils/Utils";

export function TilskuddDetaljerPage() {
  const { gjennomforingId, tilskuddId } = useRequiredParams(["gjennomforingId", "tilskuddId"]);
  const { data: tilskudd } = useTilskudd(tilskuddId);

  return (
    <TilskuddLayout gjennomforingId={gjennomforingId}>
      <VStack gap="space-16">
        <Heading level="1" size="medium">
          {tilskudd.type.navn}
        </Heading>
        <Definisjonsliste
          title="Tilskudd"
          definitions={[{ key: "Tilskuddsnummer", value: tilskudd.tilskuddsnummer }]}
        />
        <VStack gap="space-16">
          {tilskudd.vedtak.map((vedtak: TilskuddVedtak, index: number) => (
            <ExpansionCard
              key={vedtak.id}
              defaultOpen={index === 0}
              aria-label={`Vedtak ${index + 1}`}
            >
              <ExpansionCard.Header>
                <ExpansionCard.Title>
                  <VStack gap="space-2" align="start">
                    <Heading level="2" size="xsmall" spacing={false}>
                      Vedtak {index + 1}
                    </Heading>
                    <BodyShort size="small">{vedtak.vedtakResultat}</BodyShort>
                  </VStack>
                </ExpansionCard.Title>
              </ExpansionCard.Header>
              <ExpansionCard.Content>
                <VStack gap="space-16">
                  <Definisjonsliste
                    title="Vedtaksdata"
                    columns={1}
                    definitions={[
                      { key: "Vedtaksjournalpost-ID", value: vedtak.vedtakJournalpostId ?? "-" },
                      { key: "Journalpost-ID i Gosys", value: vedtak.soknadJournalpostId },
                      { key: "Søknadsdato", value: formaterDato(vedtak.soknadDato) },
                      { key: "Søknadsbeløp", value: formaterValutaBelop(vedtak.soknadBelop) },
                      { key: "Periode", value: formaterPeriode(vedtak.periode) },
                      { key: "Kostnadssted", value: vedtak.kostnadssted },
                      { key: "Vedtaksresultat", value: vedtak.vedtakResultat },
                      {
                        key: "Utbetaling mottaker",
                        value: tilskuddMottakerToString(vedtak.utbetalingMottaker),
                      },
                      { key: "KID", value: vedtak.kid ?? "-" },
                      {
                        key: "Utbetalingsbeløp",
                        value: vedtak.utbetalingBelop
                          ? formaterValutaBelop(vedtak.utbetalingBelop)
                          : "-",
                      },
                      { key: "Kommentar til brukeren", value: vedtak.kommentarVedtaksbrev ?? "-" },
                      { key: "Intern kommentar", value: vedtak.kommentarIntern ?? "-" },
                    ]}
                  />
                  <Separator />
                  <Lenke
                    to={`/gjennomforinger/${gjennomforingId}/tilskudd-behandling/${vedtak.behandlingId}`}
                  >
                    Gå til tilskuddsbehandling
                  </Lenke>
                </VStack>
              </ExpansionCard.Content>
            </ExpansionCard>
          ))}
        </VStack>
      </VStack>
    </TilskuddLayout>
  );
}
