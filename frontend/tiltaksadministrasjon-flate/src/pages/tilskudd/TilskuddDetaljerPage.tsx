import { useTilskudd } from "@/api/tilskudd/useTilskuddOrError";
import { useSimulerOpphorTilskuddVedtak } from "@/api/tilskudd/mutations";
import { TilskuddLayout } from "@/components/tilskudd/TilskuddLayout";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Alert, BodyShort, Button, ExpansionCard, HStack, Heading, VStack } from "@navikt/ds-react";
import { TilskuddHandling, TilskuddVedtak } from "@tiltaksadministrasjon/api-client";
import { tilskuddMottakerToString } from "@/utils/Utils";
import { useOpphorBrukerUtbetaling } from "@/api/tilskudd-behandling/mutations";
import { useNavigate } from "react-router";
import { useState } from "react";

export function TilskuddDetaljerPage() {
  const { gjennomforingId, tilskuddId } = useRequiredParams(["gjennomforingId", "tilskuddId"]);
  const { data: detaljer } = useTilskudd(tilskuddId);
  const { handlinger, tilskudd } = detaljer;
  const navigate = useNavigate();
  const opphorMutation = useOpphorBrukerUtbetaling();

  function opphorUtbetaling(tilskuddBehandlingId: string, tilskuddVedtakId: string) {
    opphorMutation.mutate(
      { tilskuddBehandlingId, tilskuddVedtakId },
      {
        onSuccess({ behandlingId }) {
          navigate(`/gjennomforinger/${gjennomforingId}/tilskudd-behandling/${behandlingId}`);
        },
      },
    );
  }

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
                      Vedtak {vedtak.lopenummer}
                    </Heading>
                    <BodyShort size="small">{vedtak.vedtakResultat}</BodyShort>
                  </VStack>
                </ExpansionCard.Title>
              </ExpansionCard.Header>
              <ExpansionCard.Content>
                <VStack gap="space-16">
                  <Definisjonsliste
                    title="Vedtaksdata"
                    columns={2}
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
                  <HStack justify="space-between">
                    <Lenke
                      to={`/gjennomforinger/${gjennomforingId}/tilskudd-behandling/${vedtak.behandlingId}`}
                    >
                      Gå til tilskuddsbehandling
                    </Lenke>

                    {handlinger.includes(TilskuddHandling.OPPHOR) && index === 0 && (
                      <>
                        <Button
                          type="button"
                          variant="tertiary"
                          data-color="danger"
                          onClick={() => opphorUtbetaling(vedtak.behandlingId, vedtak.id)}
                        >
                          Opphør
                        </Button>
                        <SimulerOpphorButton vedtak={vedtak} />
                      </>
                    )}
                  </HStack>
                </VStack>
              </ExpansionCard.Content>
            </ExpansionCard>
          ))}
        </VStack>
      </VStack>
    </TilskuddLayout>
  );
}

function SimulerOpphorButton({ vedtak }: { vedtak: TilskuddVedtak }) {
  const simulerOpphorMutation = useSimulerOpphorTilskuddVedtak();
  const [modalOpen, setModalOpen] = useState(false);
  const [simuleringResultat, setSimuleringResultat] = useState<unknown | null>(null);
  const [feilmelding, setFeilmelding] = useState<string | null>(null);

  function simulerOpphor() {
    setFeilmelding(null);
    setSimuleringResultat(null);
    simulerOpphorMutation.mutate(vedtak.id, {
      onSuccess: (data) => {
        setSimuleringResultat(data);
        setModalOpen(true);
      },
      onError: () => {
        setFeilmelding("Kunne ikke simulere opphør.");
      },
      onValidationError: () => {
        setFeilmelding("Kunne ikke simulere opphør.");
      },
    });
  }

  const simuleringTekst = simuleringResultat ? JSON.stringify(simuleringResultat, null, 2) : "";

  return (
    <>
      <VStack gap="space-8" align="start">
        <Button
          type="button"
          variant="tertiary"
          data-color="danger"
          loading={simulerOpphorMutation.isPending}
          onClick={simulerOpphor}
        >
          Simuler opphør
        </Button>
        {feilmelding && <Alert variant="error">{feilmelding}</Alert>}
      </VStack>
      <VarselModal
        open={modalOpen}
        handleClose={() => setModalOpen(false)}
        headingText="Simulert opphør"
        headingIconType="info"
        body={
          <VStack gap="space-16">
            <BodyShort>Resultatet av simuleringen:</BodyShort>
            <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>{simuleringTekst}</pre>
          </VStack>
        }
        primaryButton={
          <Button type="button" variant="primary" onClick={() => setModalOpen(false)}>
            Lukk
          </Button>
        }
      />
    </>
  );
}
