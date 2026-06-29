import {
  useGodkjennTilskuddBehandling,
  useReturnerTilskuddBehandling,
} from "@/api/tilskudd-behandling/mutations";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import {
  EndringshistorikkType,
  FieldError,
  TilskuddBehandlingDto,
  TilskuddBehandlingHandling,
  TilskuddBehandlingStatusAarsak,
  ValidationError,
  Valuta,
  VedtakResultat,
} from "@tiltaksadministrasjon/api-client";
import { Alert, BodyShort, Box, Button, HStack, List, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { TilskuddBehandlingLayout } from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { ToTrinnsOpprettelsesForklaring } from "../gjennomforing/tilsagn/ToTrinnsOpprettelseForklaring";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import {
  MetadataFritekstfelt,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { isAvvist } from "@/utils/totrinnskontroll";
import { DataElementStatusTag } from "@mr/frontend-common";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { TotaltBelopBox } from "@/components/tilskudd-behandling/TotaltBelopBox";
import {
  aarsakTilTekst,
  opplaeringTilskuddToString,
  tilskuddMottakerToString,
} from "@/utils/Utils";
import { PencilFillIcon } from "@navikt/aksel-icons";
import { BetalingsbetingelserEnkeltplass } from "@/components/gjennomforing/BetalingsbetingelserEnkeltplass";
import { InformasjonFraSoknad } from "@/components/tilskudd-behandling/InformasjonFraSoknad";

export function TilskuddBehandlingDetaljerPage() {
  const { gjennomforingId, behandlingId } = useRequiredParams(["gjennomforingId", "behandlingId"]);
  const { prismodell } = useEnkeltplassGjennomforingOrError(gjennomforingId);

  const {
    data: { behandling, handlinger, opprettelse },
  } = useTilskuddBehandling(behandlingId);
  const [returModalOpen, setReturModalOpen] = useState(false);
  const [attesterModalOpen, setAttesterModalOpen] = useState(false);
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

  function sendIRetur(data: {
    aarsaker: TilskuddBehandlingStatusAarsak[];
    forklaring: string | null;
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
  const kanAttesteres = handlinger.includes(TilskuddBehandlingHandling.ATTESTER);
  return (
    <TilskuddBehandlingLayout gjennomforingId={gjennomforingId}>
      {isAvvist(opprettelse) && (
        <ToTrinnsOpprettelsesForklaring
          heading="Behandlingen ble returnert"
          opprettelse={opprettelse}
        />
      )}
      <Box marginBlock="space-16">
        <HStack gap="space-8" justify="end">
          <Endringshistorikk id={behandling.id} type={EndringshistorikkType.TILSKUDD_BEHANDLING} />
          <Handlinger
            handlinger={handlinger}
            grupper={[
              {
                items: [
                  {
                    label: "Rediger tilskuddsbehandling",
                    href: "rediger",
                    handling: TilskuddBehandlingHandling.REDIGER,
                    icon: <PencilFillIcon />,
                  },
                ],
              },
            ]}
          />
        </HStack>
        <TwoColumnGrid separator>
          <>
            <Definisjonsliste
              definitions={[
                { key: "Status", value: <DataElementStatusTag {...behandling.status.status} /> },
              ]}
            />
            <Separator />
            <VStack gap="space-16">
              <InformasjonFraSoknad
                journalpostId={behandling.soknadJournalpostId}
                soknadsdato={behandling.soknadDato}
                periode={behandling.periode}
                kostnadssted={behandling.kostnadssted}
              />
              <VStack gap="space-20" align="start">
                {behandling.tilskudd.map((t) => (
                  <Box
                    className="w-full"
                    borderWidth="2"
                    borderRadius="8"
                    borderColor="neutral-subtle"
                    padding="space-8"
                    key={t.id}
                  >
                    <VStack gap="space-8">
                      <Definisjonsliste
                        definitions={[
                          {
                            key: "Tilskuddstype",
                            value: opplaeringTilskuddToString(t.tilskuddOpplaeringType),
                          },
                          {
                            key: "Hvem skal motta utbetalingen?",
                            value: tilskuddMottakerToString(t.utbetalingMottaker),
                          },
                          {
                            key: "Beløp fra søknad",
                            value: formaterValutaBelop(t.soknadBelop),
                          },
                        ]}
                      />
                      <Separator />
                      <Definisjonsliste
                        columns={1}
                        definitions={[
                          {
                            key: "Vedtaksresultat",
                            value: <DataElementStatusTag {...t.vedtakResultat.status} />,
                          },
                          {
                            key: "Beløp til utbetaling",
                            value: t.utbetalingBelop ? formaterValutaBelop(t.utbetalingBelop) : "-",
                          },
                          { key: "Kommentar til brukeren", value: t.kommentarVedtaksbrev },
                        ]}
                      />
                    </VStack>
                  </Box>
                ))}
              </VStack>
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
              <MetadataFritekstfelt
                label="Kommentar (internt i Nav)"
                value={behandling.kommentarIntern}
              />
            </VStack>
          </>
          <BetalingsbetingelserEnkeltplass prismodell={prismodell} />
        </TwoColumnGrid>
      </Box>
      <Separator />
      {(kanReturneres || kanAttesteres) && (
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
          {kanAttesteres && (
            <Button
              variant="primary"
              size="small"
              type="button"
              onClick={() => setAttesterModalOpen(true)}
            >
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
      <AarsakerOgForklaringModal<TilskuddBehandlingStatusAarsak>
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
        header="Send i retur med forklaring"
        buttonLabel="Send i retur"
        open={returModalOpen}
        onClose={() => setReturModalOpen(false)}
        errors={errors}
        onConfirm={sendIRetur}
      />
      <VarselModal
        open={attesterModalOpen}
        handleClose={() => setAttesterModalOpen(false)}
        headingText="Attester tilskuddsbehandling"
        headingIconType="info"
        body={attesterModalInnhold(behandling)}
        secondaryButton
        primaryButton={
          <Button variant="primary" onClick={attester}>
            Ja, attester behandling
          </Button>
        }
      />
    </TilskuddBehandlingLayout>
  );
}

function attesterModalInnhold(behandling: TilskuddBehandlingDto) {
  return (
    <>
      <BodyShort spacing>Du er i ferd med å attestere vedtak om:</BodyShort>
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
