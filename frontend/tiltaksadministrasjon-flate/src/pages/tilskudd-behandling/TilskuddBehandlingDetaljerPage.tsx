import {
  useGodkjennTilskuddBehandling,
  useReturnerTilskuddBehandling,
} from "@/api/tilskudd-behandling/mutations";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { AarsakerOgBegrunnelseModal } from "@/components/modal/AarsakerOgBegrunnelseModal";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import {
  FieldError,
  TilskuddBehandlingDto,
  TilskuddBehandlingHandling,
  TilskuddBehandlingStatusAarsak,
  ValidationError,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { Alert, BodyShort, Box, Button, Heading, HStack, List, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { TilskuddBehandlingLayout } from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { TotrinnskontrollReturnert } from "@/components/totrinnskontroll/TotrinnskontrollReturnert";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { erReturnert } from "@/utils/totrinnskontroll";
import { DataElementStatusTag } from "@mr/frontend-common";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { TotaltBelopBox } from "@/components/tilskudd-behandling/TotaltBelopBox";
import {
  aarsakTilTekst,
  opplaeringTilskuddToString,
  tilskuddMottakerToString,
} from "@/utils/Utils";
import { Saksopplysninger } from "@/components/tilskudd-behandling/Saksopplysninger";
import { VedtakDetaljer } from "@/components/tilskudd-behandling/VedtakDetaljer";
import { TilskuddBehandlingHandlinger } from "./TilskuddBehandlingHandlinger";
import { TilskuddFormGroup } from "@/layouts/TilskuddFormGroup";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { PrisOgBetalingsbetingelser } from "@/components/prismodell/PrisOgBetalingsbetingelser";

export function TilskuddBehandlingDetaljerPage() {
  const { gjennomforingId, behandlingId } = useRequiredParams(["gjennomforingId", "behandlingId"]);
  const { prismodell } = useEnkeltplassGjennomforingOrError(gjennomforingId);

  const {
    data: { behandling, handlinger, opprettelse },
  } = useTilskuddBehandling(behandlingId);

  const [returModalOpen, setReturModalOpen] = useState(false);
  const [godkjennModalOpen, setGodkjennModalOpen] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);
  const navigate = useNavigate();

  const godkjennMutation = useGodkjennTilskuddBehandling(gjennomforingId);
  const returnerMutation = useReturnerTilskuddBehandling(gjennomforingId);
  const listUrl = `/gjennomforinger/${gjennomforingId}/tilskudd-behandling`;

  function godkjenn() {
    godkjennMutation.mutate(behandling.id, {
      onSuccess: () => navigate(listUrl),
      onValidationError: (error: ValidationError) => setErrors(error.errors),
    });
  }

  function sendIRetur(data: {
    aarsaker: TilskuddBehandlingStatusAarsak[];
    begrunnelse: string | null;
  }) {
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

  const kanReturneres = handlinger.includes(TilskuddBehandlingHandling.RETURNER);
  const kanGodkjennes = handlinger.includes(TilskuddBehandlingHandling.GODKJENN);
  return (
    <TilskuddBehandlingLayout gjennomforingId={gjennomforingId}>
      <TilskuddBehandlingHandlinger tilskuddBehandlingId={behandlingId} />
      {erReturnert(opprettelse) && (
        <Box marginBlock="space-0 space-16">
          <TotrinnskontrollReturnert
            heading="Behandlingen ble returnert"
            opprettelse={opprettelse}
          />
        </Box>
      )}
      <TwoColumnGrid separator>
        <VStack gap="space-20">
          <HStack gap="space-8" align="center">
            <Heading level="3" size="medium">
              Vedtak
            </Heading>
            <DataElementStatusTag {...behandling.status.status} />
          </HStack>
          {behandling.tilskudd.map((t) => (
            <TilskuddFormGroup key={t.id}>
              <>
                <Heading size="small" level="3" spacing>
                  {`${opplaeringTilskuddToString(t.tilskuddOpplaeringType)} for perioden ${formaterPeriode(t.periode)}`}
                </Heading>
                <Separator />
              </>
              <Saksopplysninger
                journalpostId={t.soknadJournalpostId}
                soknadsdato={t.soknadDato}
                periode={t.periode}
                kostnadssted={t.kostnadssted}
                belop={t.soknadBelop.belop || 0}
                tilskuddOpplaeringType={t.tilskuddOpplaeringType}
                utbetalingMottaker={t.utbetalingMottaker}
              />
              <Separator />
              <VedtakDetaljer
                vedtakResultat={t.vedtakResultat}
                utbetalingBelop={t.utbetalingBelop}
                kommentarVedtaksbrev={t.kommentarVedtaksbrev}
                internKommentar={t.kommentarIntern}
              />
            </TilskuddFormGroup>
          ))}
          <TotaltBelopBox
            label="Totalt beløp fra søknad"
            belop={{
              belop: behandling.tilskudd.reduce((sum, t) => sum + t.soknadBelop.belop, 0),
              valuta: behandling.tilskudd.at(0)?.soknadBelop.valuta ?? Valuta.NOK,
            }}
          />
          <TotaltBelopBox
            label="Totalt beløp til utbetaling"
            belop={{
              belop: behandling.tilskudd.reduce(
                (sum, t) => sum + (t.utbetalingBelop?.belop ?? 0),
                0,
              ),
              valuta: Valuta.NOK,
            }}
          />
        </VStack>
        <Box>
          <PrisOgBetalingsbetingelser prismodell={prismodell} size="medium" />
        </Box>
      </TwoColumnGrid>
      <Separator />
      {(kanReturneres || kanGodkjennes) && (
        <HStack gap="space-8" marginBlock="space-16" justify="end">
          {kanReturneres && (
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setReturModalOpen(true)}
            >
              Send i retur
            </Button>
          )}
          {kanGodkjennes && (
            <Button
              variant="primary"
              size="small"
              type="button"
              onClick={() => setGodkjennModalOpen(true)}
            >
              Godkjenn
            </Button>
          )}
        </HStack>
      )}
      {errors.map((error) => (
        <Alert className="self-end" variant="error" size="small">
          {error.detail}
        </Alert>
      ))}
      <AarsakerOgBegrunnelseModal<TilskuddBehandlingStatusAarsak>
        aarsaker={[
          {
            value: TilskuddBehandlingStatusAarsak.FEIL_SAKSOPPLYSNINGER,
            label: aarsakTilTekst(TilskuddBehandlingStatusAarsak.FEIL_SAKSOPPLYSNINGER),
          },
          {
            value: TilskuddBehandlingStatusAarsak.FEIL_BELOP,
            label: aarsakTilTekst(TilskuddBehandlingStatusAarsak.FEIL_BELOP),
          },
          {
            value: TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT,
            label: aarsakTilTekst(TilskuddBehandlingStatusAarsak.FEIL_VEDTAKSRESULTAT),
          },
          {
            value: TilskuddBehandlingStatusAarsak.ANNET,
            label: aarsakTilTekst(TilskuddBehandlingStatusAarsak.ANNET),
          },
        ]}
        header="Send i retur med begrunnelse"
        buttonLabel="Send i retur"
        open={returModalOpen}
        onClose={() => setReturModalOpen(false)}
        errors={errors}
        onConfirm={sendIRetur}
      />
      <VarselModal
        open={godkjennModalOpen}
        handleClose={() => setGodkjennModalOpen(false)}
        headingText="Godkjenn tilskuddsbehandling"
        headingIconType="info"
        body={godkjennModalInnhold(behandling)}
        secondaryButton
        primaryButton={
          <Button variant="primary" onClick={godkjenn}>
            Ja, godkjenn behandling
          </Button>
        }
      />
    </TilskuddBehandlingLayout>
  );
}

function godkjennModalInnhold(behandling: TilskuddBehandlingDto) {
  return (
    <>
      <BodyShort spacing>Du er i ferd med å godkjenne vedtak om:</BodyShort>
      <List>
        {behandling.tilskudd.map((t) =>
          t.vedtakResultat.type === VedtakResultat.INNVILGELSE ? (
            <List.Item key={t.id}>
              {t.utbetalingBelop?.belop ?? 0} {(t.utbetalingBelop?.valuta ?? Valuta.NOK).toString()}{" "}
              i {opplaeringTilskuddToString(t.tilskuddOpplaeringType).toLowerCase()} som{" "}
              {tilskuddMottakerToString(t.utbetalingMottaker).toLowerCase()}
            </List.Item>
          ) : (
            <List.Item key={t.id}>
              {" "}
              Avslag på {opplaeringTilskuddToString(t.tilskuddOpplaeringType).toLowerCase()}
            </List.Item>
          ),
        )}
      </List>
    </>
  );
}
