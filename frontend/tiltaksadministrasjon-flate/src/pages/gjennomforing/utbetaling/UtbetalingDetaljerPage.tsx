import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import {
  AarsakerOgForklaringRequestUtbetalingStatusAarsak,
  EndringshistorikkType,
  FieldError,
  OpprettUtbetalingLinjerRequest,
  TilsagnType,
  Tilskuddstype,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinjeDto,
  UtbetalingStatusAarsak,
  UtbetalingStatusDtoType,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import {
  BodyShort,
  Box,
  Button,
  CopyButton,
  Heading,
  HelpText,
  HGrid,
  HStack,
  Link,
  Modal,
  VStack,
} from "@navikt/ds-react";
import { Link as ReactRouterLink, useNavigate } from "react-router";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import {
  formaterDato,
  formaterPeriode,
  subDuration,
  yyyyMMddFormatting,
} from "@mr/frontend-common/utils/date";
import {
  useUtbetaling,
  useUtbetalingBeregning,
  useUtbetalingsLinjer,
} from "./utbetalingPageLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import {
  MetadataFritekstfelt,
  MetadataVStack,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { UtbetalingTypeTag } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { BetalingsinformasjonDetaljer } from "@/components/utbetaling/BetalingsinformasjonDetaljer";
import { useState } from "react";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import {
  FileCheckmarkIcon,
  PencilIcon,
  PiggybankIcon,
  PlusCircleIcon,
  TrashFillIcon,
  XMarkIcon,
} from "@navikt/aksel-icons";
import { Handlinger } from "@/components/handlinger/Handlinger";
import { OpprettKorreksjonModal } from "@/components/utbetaling/OpprettKorreksjonModal";
import { useAvbrytUtbetaling, useSlettKorreksjon } from "@/api/utbetaling/mutations";
import { useForm, UseFormReturn } from "react-hook-form";

function useUtbetalingDetaljerData() {
  const { utbetalingId } = useRequiredParams(["utbetalingId"]);
  const { utbetaling, handlinger } = useUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);
  return { utbetaling, handlinger, beregning };
}

export function UtbetalingDetaljerPage() {
  const navigate = useNavigate();
  const [modalVariant, setModalVariant] = useState<UtbetalingHandling | null>(null);

  const { utbetaling, handlinger, beregning } = useUtbetalingDetaljerData();
  const { data: utbetalingLinjer } = useUtbetalingsLinjer(utbetaling.id);

  const { gjennomforingId, periode, tilskuddstype } = utbetaling;

  const tilsagnsTypeFraTilskudd = tilsagnType(tilskuddstype);

  function opprettEkstraTilsagn() {
    const defaultTilsagn = utbetalingLinjer.length === 1 ? utbetalingLinjer[0].tilsagn : undefined;
    return navigate(
      `/gjennomforinger/${gjennomforingId}/tilsagn/opprett-tilsagn` +
        `?type=${tilsagnsTypeFraTilskudd}` +
        `&periodeStart=${periode.start}` +
        `&periodeSlutt=${yyyyMMddFormatting(subDuration(periode.slutt, { days: 1 }))}` +
        `&kostnadssted=${defaultTilsagn?.kostnadssted.enhetsnummer || ""}`,
    );
  }

  const form: UseFormReturn<OpprettUtbetalingLinjerRequest> =
    useForm<OpprettUtbetalingLinjerRequest>({
      defaultValues: {
        utbetalingId: utbetaling.id,
        utbetalingLinjer: utbetalingLinjer.map((linje) => ({
          id: linje.id,
          tilsagnId: linje.tilsagn.id,
          pris: linje.pris,
          gjorOppTilsagn: linje.gjorOppTilsagn,
        })),
        begrunnelseMindreBetalt: null,
      },
    });

  return (
    <VStack className="pb-6">
      <HStack justify="end">
        <Endringshistorikk id={utbetaling.id} type={EndringshistorikkType.UTBETALING} />
        <Handlinger
          handlinger={handlinger}
          grupper={[
            {
              items: [
                {
                  label: "Opprett korreksjon",
                  onClick: () => setModalVariant(UtbetalingHandling.OPPRETT_KORREKSJON),
                  icon: <PlusCircleIcon />,
                  handling: UtbetalingHandling.OPPRETT_KORREKSJON,
                },
              ],
            },
            {
              items: [
                {
                  handling: UtbetalingHandling.AVBRYT,
                  label: utbetalingTekster.avbrutt.handling.button.label,
                  onClick: () => setModalVariant(UtbetalingHandling.AVBRYT),
                  variant: "danger",
                  icon: <XMarkIcon />,
                },
                {
                  handling: UtbetalingHandling.REDIGER,
                  label: "Rediger utbetaling",
                  href: "rediger-utbetaling",
                  icon: <PencilIcon />,
                },
                {
                  handling: UtbetalingHandling.OPPRETT_TILSAGN,
                  label: utbetalingTekster.linje.handlinger.opprettTilsagn(tilsagnsTypeFraTilskudd),
                  onClick: opprettEkstraTilsagn,
                  icon: <PiggybankIcon />,
                },
                {
                  handling: UtbetalingHandling.HENT_GODKJENTE_TILSAGN,
                  label: utbetalingTekster.linje.handlinger.hentGodkjenteTilsagn,
                  onClick: () =>
                    form.setValue(
                      "utbetalingLinjer",
                      utbetalingLinjer.map((linje) => ({
                        id: linje.id,
                        pris: linje.pris,
                        tilsagnId: linje.tilsagn.id,
                        gjorOppTilsagn: linje.gjorOppTilsagn,
                      })),
                    ),
                  icon: <FileCheckmarkIcon />,
                },
                {
                  handling: UtbetalingHandling.SLETT,
                  label: "Slett utbetaling",
                  onClick: () => setModalVariant(UtbetalingHandling.SLETT),
                  icon: <TrashFillIcon />,
                },
              ],
            },
          ]}
        />
        <UtbetalingAvbrytModal
          utbetalingId={utbetaling.id}
          open={modalVariant === UtbetalingHandling.AVBRYT}
          onClose={() => setModalVariant(null)}
        />
        <OpprettKorreksjonModal
          utbetaling={utbetaling}
          open={modalVariant === UtbetalingHandling.OPPRETT_KORREKSJON}
          close={() => setModalVariant(null)}
        />
        <SlettKorreksjonModal
          utbetalingId={utbetaling.id}
          open={modalVariant === UtbetalingHandling.SLETT}
          onClose={() => setModalVariant(null)}
        />
      </HStack>
      <Separator />
      <VStack gap="space-12">
        <HGrid columns="1fr auto" align="start">
          <TwoColumnGrid separator>
            <Box>
              <Heading size="medium" spacing level="3" data-testid="utbetaling-til-utbetaling">
                Detaljer
              </Heading>
              <VStack gap="space-16">
                <MetadataVStack
                  label={utbetalingTekster.metadata.status}
                  value={<UtbetalingStatusTag status={utbetaling.status} />}
                />
                {utbetaling.avbruttBegrunnelse && (
                  <MetadataFritekstfelt
                    label={utbetalingTekster.metadata.avbruttBegrunnelse}
                    value={utbetaling.avbruttBegrunnelse}
                  />
                )}
                <MetadataVStack
                  label={utbetalingTekster.metadata.periode}
                  value={formaterPeriode(utbetaling.periode)}
                />
                <MetadataVStack
                  label={utbetalingTekster.beregning.belop.label}
                  value={formaterValutaBelop(utbetaling.beregning)}
                />
                {utbetaling.type.tagName && (
                  <HGrid columns="1fr 1fr" gap="space-24">
                    <MetadataVStack
                      label={utbetalingTekster.metadata.type}
                      value={
                        <HStack gap="space-4">
                          {utbetaling.type.displayName}
                          <UtbetalingTypeTag type={utbetaling.type.displayName} />
                        </HStack>
                      }
                    />
                    {utbetaling.korreksjon?.opprinneligUtbetaling && (
                      <MetadataVStack
                        label={utbetalingTekster.korreksjon.gjelderUtbetaling}
                        value={
                          <Link
                            as={ReactRouterLink}
                            to={`/gjennomforinger/${utbetaling.gjennomforingId}/utbetalinger/${utbetaling.korreksjon.opprinneligUtbetaling}`}
                          >
                            Opprinnelig utbetaling
                          </Link>
                        }
                      />
                    )}
                  </HGrid>
                )}
                {utbetaling.korreksjon && (
                  <MetadataFritekstfelt
                    label={utbetalingTekster.korreksjon.begrunnelse}
                    value={utbetaling.korreksjon.begrunnelse}
                  />
                )}
                {utbetaling.utbetalesTidligstDato && (
                  <MetadataVStack
                    label={
                      <HStack align="center" gap="space-4">
                        {utbetalingTekster.metadata.utbetalesTidligstDato}
                        <HelpText>
                          {utbetalingTekster.metadata.utbetalesTidligstDatoHelpText}
                        </HelpText>
                      </HStack>
                    }
                    value={formaterDato(utbetaling.utbetalesTidligstDato)}
                  />
                )}
              </VStack>
            </Box>
            <Box>
              <Heading size="medium" level="3" spacing>
                Betalingsinformasjon
              </Heading>
              <VStack gap="space-16">
                {utbetaling.betalingsinformasjon && (
                  <BetalingsinformasjonDetaljer
                    betalingsinformasjon={utbetaling.betalingsinformasjon}
                  />
                )}
                {utbetaling.journalpostId && (
                  <MetadataVStack
                    label="Journalpost-ID i Gosys"
                    value={
                      <HStack align="center">
                        <CopyButton
                          size="small"
                          copyText={utbetaling.journalpostId}
                          title="Kopier journalpost-ID"
                        />
                        {utbetaling.journalpostId}
                      </HStack>
                    }
                  />
                )}

                {utbetaling.begrunnelseMindreBetalt && (
                  <MetadataFritekstfelt
                    label={utbetalingTekster.metadata.begrunnelseMindreBetalt}
                    value={utbetaling.begrunnelseMindreBetalt}
                  />
                )}
                {utbetaling.kommentar && (
                  <MetadataFritekstfelt
                    label={utbetalingTekster.metadata.kommentar}
                    value={utbetaling.kommentar}
                  />
                )}
              </VStack>
            </Box>
          </TwoColumnGrid>
        </HGrid>
        <UtbetalingBeregningView utbetalingId={utbetaling.id} beregning={beregning} />
        <UtbetalingLinjeView
          utbetaling={utbetaling}
          utbetalingLinjer={utbetalingLinjer}
          handlinger={handlinger}
          form={form}
        />
      </VStack>
    </VStack>
  );
}

interface UtbetalingLinjeViewProps {
  utbetaling: UtbetalingDto;
  utbetalingLinjer: UtbetalingLinjeDto[];
  handlinger: UtbetalingHandling[];
  form: UseFormReturn<OpprettUtbetalingLinjerRequest>;
}

function UtbetalingLinjeView({
  utbetaling,
  utbetalingLinjer,
  handlinger,
  form,
}: UtbetalingLinjeViewProps) {
  switch (utbetaling.status.type) {
    case UtbetalingStatusDtoType.VENTER_PA_ARRANGOR:
    case UtbetalingStatusDtoType.UBEHANDLET_FORSLAG:
    case UtbetalingStatusDtoType.AVBRUTT:
      return null;

    case UtbetalingStatusDtoType.RETURNERT:
    case UtbetalingStatusDtoType.KLAR_TIL_BEHANDLING:
      return (
        <RedigerUtbetalingLinjeView
          utbetaling={utbetaling}
          handlinger={handlinger}
          utbetalingLinjer={utbetalingLinjer}
          form={form}
        />
      );

    case UtbetalingStatusDtoType.TIL_ATTESTERING:
    case UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING:
    case UtbetalingStatusDtoType.DELVIS_UTBETALT:
    case UtbetalingStatusDtoType.UTBETALT:
      return <BesluttUtbetalingLinjeView utbetaling={utbetaling} />;
  }
}

function tilsagnType(tilskuddstype: Tilskuddstype): TilsagnType {
  switch (tilskuddstype) {
    case Tilskuddstype.TILTAK_DRIFTSTILSKUDD:
    case Tilskuddstype.TILTAK_OPPLAERING_TILSKUDD:
      return TilsagnType.EKSTRATILSAGN;
    case Tilskuddstype.TILTAK_INVESTERINGER:
      return TilsagnType.INVESTERING;
  }
}

interface UtbetalingAvbrytModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

function UtbetalingAvbrytModal({ utbetalingId, open, onClose }: UtbetalingAvbrytModalProps) {
  const [errors, setErrors] = useState<FieldError[]>([]);
  const avbrytUtbetalingMutation = useAvbrytUtbetaling();

  function avbrytUtbetaling(body: AarsakerOgForklaringRequestUtbetalingStatusAarsak) {
    avbrytUtbetalingMutation.mutate(
      { id: utbetalingId, body },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
        onSuccess: () => {
          onClose();
        },
      },
    );
  }

  const avbrytUtbetalingAarsakValg = [
    UtbetalingStatusAarsak.TILSAGN_GJORT_OPP,
    UtbetalingStatusAarsak.ANNET,
  ].map((val) => {
    return {
      value: val,
      label: utbetalingTekster.avbrutt.aarsak.fraAarsak(val),
    };
  });
  return (
    <AarsakerOgForklaringModal<UtbetalingStatusAarsak>
      width={750}
      open={open}
      onClose={onClose}
      header={utbetalingTekster.avbrutt.aarsak.modal.header}
      ingress={<BodyShort>{utbetalingTekster.avbrutt.aarsak.modal.ingress}</BodyShort>}
      aarsaker={avbrytUtbetalingAarsakValg}
      buttonLabel={utbetalingTekster.avbrutt.aarsak.modal.button.label}
      errors={errors}
      onConfirm={(request) => avbrytUtbetaling(request)}
    />
  );
}

interface SlettKorreksjonModalProps {
  utbetalingId: string;
  open: boolean;
  onClose: () => void;
}

function SlettKorreksjonModal({ utbetalingId, open, onClose }: SlettKorreksjonModalProps) {
  const navigate = useNavigate();
  const slettKorreksjonMutation = useSlettKorreksjon();

  function slettKorreksjon() {
    slettKorreksjonMutation.mutate(
      { id: utbetalingId },
      {
        onSuccess: () => navigate("..", { replace: true }),
      },
    );
  }

  return (
    <Modal onClose={onClose} closeOnBackdropClick aria-label="modal" open={open}>
      <Modal.Header closeButton={false}>
        <Heading align="start" size="medium">
          Slett utbetaling
        </Heading>
      </Modal.Header>
      <Modal.Body>
        <BodyShort>
          Du er i ferd med å slette en korrigeringsutbetaling. Dette vil fjerne den valgte
          ubetalingen fra løsningen. Er du sikker på at du vil fortsette?
        </BodyShort>
      </Modal.Body>
      <Modal.Footer>
        <HStack gap="space-16">
          <Button type="button" variant="secondary" onClick={onClose}>
            Nei, takk
          </Button>
          <Button
            data-color="danger"
            title="Slett utbetaling"
            variant="primary"
            onClick={slettKorreksjon}
          >
            Ja, jeg vil slette utbetalingen
          </Button>
        </HStack>
      </Modal.Footer>
    </Modal>
  );
}
