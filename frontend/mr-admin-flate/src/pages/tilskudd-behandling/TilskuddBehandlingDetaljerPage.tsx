import {
  useGodkjennTilskuddBehandling,
  useReturnerTilskuddBehandling,
} from "@/api/tilskudd-behandling/mutations";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import {
  Besluttelse,
  DocumentClass,
  FieldError,
  TilskuddBehandlingHandling,
  TilskuddBehandlingStatus,
  ValidationError,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import {
  ActionMenu,
  Alert,
  BodyShort,
  Box,
  Button,
  Heading,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { TilskuddBehandlingLayout } from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { useEndringshistorikk } from "@/api/endringshistorikk/useEndringshistorikk";
import { ToTrinnsOpprettelsesForklaring } from "../gjennomforing/tilsagn/ToTrinnsOpprettelseForklaring";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { DeltakerinformasjonOgBetalingsbetingelser } from "@/components/tilskudd-behandling/DeltakerinformasjonOgBetalingsbetingelser";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { PadlockLockedIcon } from "@navikt/aksel-icons";
import { isBesluttet } from "@/utils/totrinnskontroll";

export function TilskuddBehandlingDetaljerPage() {
  const { gjennomforingId, behandlingId } = useRequiredParams(["gjennomforingId", "behandlingId"]);
  const { prismodell, enkeltplassDeltaker } = useEnkeltplassGjennomforingOrError(gjennomforingId);

  const {
    data: { behandling, handlinger, opprettelse },
  } = useTilskuddBehandling(behandlingId);
  const { data: historikk } = useEndringshistorikk(
    behandling.id,
    DocumentClass.TILSKUDD_BEHANDLING,
  );
  const [returModalOpen, setReturModalOpen] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);
  const navigate = useNavigate();

  const godkjennMutation = useGodkjennTilskuddBehandling(gjennomforingId);
  const returnerMutation = useReturnerTilskuddBehandling(gjennomforingId);

  const listUrl = `/gjennomforinger/${gjennomforingId}/tilskudd-behandling`;

  function attester() {
    godkjennMutation.mutate(behandling.id, {
      onSuccess: () => navigate(listUrl),
      onValidationError: (error: ValidationError) => setErrors(error.errors),
    });
  }

  function sendIRetur(data: { aarsaker: string[]; forklaring: string | null }) {
    returnerMutation.mutate(
      { id: behandling.id, body: { ...data } },
      {
        onSuccess: () => {
          setReturModalOpen(false);
          navigate(listUrl);
        },
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  const erTilAttestering = behandling.status.type === TilskuddBehandlingStatus.TIL_ATTESTERING;

  return (
    <TilskuddBehandlingLayout gjennomforingId={gjennomforingId}>
      <>
        {isBesluttet(opprettelse) && opprettelse.besluttelse === Besluttelse.AVVIST && (
          <ToTrinnsOpprettelsesForklaring
            heading="Behandlingen ble returnert"
            opprettelse={opprettelse}
          />
        )}
        <Box marginBlock="space-16">
          <HStack gap="space-8" justify="end">
            <EndringshistorikkPopover>
              <ViewEndringshistorikk historikk={historikk} />
            </EndringshistorikkPopover>
            <Handlinger>
              {handlinger.includes(TilskuddBehandlingHandling.REDIGER) && (
                <ActionMenu.Item onSelect={() => navigate("rediger")}>Rediger</ActionMenu.Item>
              )}
            </Handlinger>
          </HStack>
          <TwoColumnGrid separator>
            <>
              <Heading size="small" level="3" spacing>
                Informasjon fra søknad
              </Heading>
              <VStack gap="space-16">
                <Definisjonsliste
                  definitions={[
                    { key: "JournalpostID", value: behandling.soknadJournalpostId },
                    { key: "Søknadsdato", value: formaterDato(behandling.soknadDato) },
                    { key: "Periode", value: formaterPeriode(behandling.periode) },
                    { key: "Kostnadssted", value: behandling.kostnadssted },
                  ]}
                />
                <VStack gap="space-20" align="start">
                  {behandling.vedtak.map((v) => (
                    <Box
                      className="w-full"
                      borderWidth="2"
                      borderRadius="8"
                      borderColor="neutral-subtle"
                      padding="space-8"
                      key={v.id}
                    >
                      <VStack gap="space-8">
                        <Definisjonsliste
                          definitions={[
                            { key: "Tilskuddstype", value: v.tilskuddOpplaeringType },
                            { key: "Hvem skal motta utbetalingen?", value: v.utbetalingMottaker },
                            {
                              key: "Beløp fra søknad",
                              value: formaterValuta(v.soknadBelop, v.soknadValuta),
                            },
                          ]}
                        />
                        <Separator />
                        <Definisjonsliste
                          columns={1}
                          definitions={[
                            { key: "Vedtaksresultat", value: v.vedtakResultat },
                            { key: "Kommentar til brukeren", value: v.kommentarVedtaksbrev },
                          ]}
                        />
                      </VStack>
                    </Box>
                  ))}
                </VStack>
                <Box
                  className="w-full"
                  borderWidth="2"
                  borderRadius="8"
                  borderColor="neutral-subtle"
                  padding="space-8"
                >
                  <HStack justify="space-between">
                    <HStack align="center" gap="space-8">
                      <PadlockLockedIcon title="a11y-title" fontSize="1.5rem" />
                      <BodyShort size="large">Totalt beløp fra søknad</BodyShort>
                    </HStack>
                    <BodyShort size="large">
                      {formaterValuta(
                        behandling.vedtak.reduce((sum, v) => sum + v.soknadBelop, 0),
                        Valuta.NOK,
                      )}
                    </BodyShort>
                  </HStack>
                </Box>
              </VStack>
            </>
            <DeltakerinformasjonOgBetalingsbetingelser
              deltaker={enkeltplassDeltaker}
              prisbetingelser={prismodell.prisbetingelser}
            />
          </TwoColumnGrid>
        </Box>
        <Separator />
        <>
          {erTilAttestering && (
            <HStack gap="space-8" marginBlock="space-16" justify="end">
              {handlinger.includes(TilskuddBehandlingHandling.RETURNER) && (
                <Button
                  variant="secondary"
                  size="small"
                  type="button"
                  onClick={() => setReturModalOpen(true)}
                >
                  Send i retur
                </Button>
              )}
              {handlinger.includes(TilskuddBehandlingHandling.ATTESTER) && (
                <Button variant="primary" size="small" type="button" onClick={attester}>
                  Attester
                </Button>
              )}
            </HStack>
          )}
          {errors.map((error) => (
            <Alert className="self-end" variant="error" size="small">
              {error.detail}
            </Alert>
          ))}
          <AarsakerOgForklaringModal<string>
            aarsaker={[{ value: "ANNET", label: "Annet" }]}
            header="Send i retur med forklaring"
            buttonLabel="Send i retur"
            open={returModalOpen}
            onClose={() => setReturModalOpen(false)}
            errors={errors}
            onConfirm={sendIRetur}
          />
        </>
      </>
    </TilskuddBehandlingLayout>
  );
}
