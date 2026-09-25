import { useTilskudd } from "@/api/tilskudd/useTilskuddOrError";
import { useSimulerOpphorTilskuddVedtak } from "@/api/tilskudd/mutations";
import { TilskuddLayout } from "@/components/tilskudd/TilskuddLayout";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { Definition } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { MetadataVStack, Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Lenke } from "@mr/frontend-common/components/lenke/Lenke";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { TilskuddVedtakUtbetaling } from "@tiltaksadministrasjon/api-client";
import {
  Alert,
  BodyShort,
  Button,
  ExpansionCard,
  HStack,
  Heading,
  Modal,
  TextField,
  VStack,
} from "@navikt/ds-react";
import {
  HelVedSimuleringResponse,
  TilskuddHandling,
  TilskuddVedtak,
} from "@tiltaksadministrasjon/api-client";
import { tilskuddMottakerToString } from "@/utils/Utils";
import { useOpphorBrukerUtbetaling } from "@/api/tilskudd-behandling/mutations";
import { useNavigate } from "react-router";
import { SubmitEvent, useState } from "react";

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
                      { key: "Kommentar til brukeren", value: vedtak.kommentarVedtaksbrev ?? "-" },
                      { key: "Intern kommentar", value: vedtak.kommentarIntern ?? "-" },
                    ]}
                  />
                  <Separator />
                  {vedtak.utbetaling && <VedtaksUtbetalingInfo utbetaling={vedtak.utbetaling} />}
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
                        <SimulerButton gjennomforingId={gjennomforingId} vedtak={vedtak} />
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

interface VedtaksUtbetalingInfoProps {
  utbetaling: TilskuddVedtakUtbetaling;
}

function VedtaksUtbetalingInfo({ utbetaling }: VedtaksUtbetalingInfoProps) {
  return (
    <Definisjonsliste
      title="Utbetaling info"
      columns={2}
      definitions={utbetalingInfo(utbetaling)}
    />
  );
}

function utbetalingInfo(utbetaling: TilskuddVedtakUtbetaling): Definition[] {
  switch (utbetaling.type) {
    case "BRUKER":
      return [
        {
          key: "Utbetalingsbeløp",
          value: formaterValutaBelop(utbetaling.belop),
        },
      ];
    case "ARRANGOR":
      return [
        {
          key: "Utbetalingsbeløp",
          value: formaterValutaBelop(utbetaling.belop),
        },
        { key: "KID", value: utbetaling.kid ?? "-" },
      ];
  }
}

function SimulerButton({
  gjennomforingId,
  vedtak,
}: {
  gjennomforingId: string;
  vedtak: TilskuddVedtak;
}) {
  const simulerOpphorMutation = useSimulerOpphorTilskuddVedtak();
  const [modalOpen, setModalOpen] = useState(false);
  const [belop, setBelop] = useState("0");
  const [simuleringResultat, setSimuleringResultat] = useState<HelVedSimuleringResponse | null>(
    null,
  );
  const [feilmelding, setFeilmelding] = useState<string | null>(null);
  const formId = `simuler-opphor-form-${vedtak.id}`;

  function openModal() {
    setBelop("0");
    setFeilmelding(null);
    setSimuleringResultat(null);
    setModalOpen(true);
  }

  function closeModal() {
    setModalOpen(false);
    setBelop("0");
    setFeilmelding(null);
    setSimuleringResultat(null);
    simulerOpphorMutation.reset();
  }

  function simulerOpphor(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    setFeilmelding(null);
    setSimuleringResultat(null);

    const parsedBelop = Number(belop);
    if (!Number.isInteger(parsedBelop) || parsedBelop < 0) {
      setFeilmelding("Beløp må være 0 eller større.");
      return;
    }

    simulerOpphorMutation.mutate(
      { gjennomforingId, vedtakId: vedtak.id, belop: parsedBelop },
      {
        onSuccess: (data) => {
          setSimuleringResultat(data);
        },
        onError: () => {
          setFeilmelding("Kunne ikke simulere opphør.");
        },
        onValidationError: () => {
          setFeilmelding("Kunne ikke simulere opphør.");
        },
      },
    );
  }

  return (
    <>
      <Button
        type="button"
        variant="tertiary"
        data-color="accent"
        loading={simulerOpphorMutation.isPending}
        onClick={openModal}
      >
        Simuler utbetaling
      </Button>
      <Modal
        open={modalOpen}
        onClose={closeModal}
        closeOnBackdropClick
        header={{ heading: "Simuler utbetaling" }}
        width="medium"
      >
        <Modal.Body>
          <form id={formId} onSubmit={simulerOpphor}>
            <VStack gap="space-16">
              <MetadataVStack
                label="Vedtak utbetalt beløp"
                value={vedtak.utbetaling?.belop.belop}
              />
              <BodyShort>Fyll inn beløpet du vil simulere utbetaling med</BodyShort>
              <TextField
                label="Beløp"
                type="number"
                min={0}
                step={1}
                value={belop}
                onChange={(event) => setBelop(event.currentTarget.value)}
              />
              {simuleringResultat && (
                <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                  {JSON.stringify(simuleringResultat, null, 2)}
                </pre>
              )}
            </VStack>
          </form>
        </Modal.Body>
        <Modal.Footer>
          <VStack gap="space-4">
            <HStack gap="space-4">
              <Button form={formId} type="submit" loading={simulerOpphorMutation.isPending}>
                Simuler opphør
              </Button>
              <Button type="button" variant="tertiary" onClick={closeModal}>
                Avbryt
              </Button>
            </HStack>
            {feilmelding && <Alert variant="error">{feilmelding}</Alert>}
          </VStack>
        </Modal.Footer>
      </Modal>
    </>
  );
}
