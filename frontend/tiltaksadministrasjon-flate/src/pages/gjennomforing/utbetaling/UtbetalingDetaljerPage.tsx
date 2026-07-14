import { Endringshistorikk } from "@/components/endringshistorikk/Endringshistorikk";
import {
  EndringshistorikkType,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingStatusDtoType,
  Tilskuddstype,
  TilsagnType,
  UtbetalingLinjeDto,
  OpprettUtbetalingLinjerRequest,
  FieldError,
  ValidationError,
  UtbetalingStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import {
  Box,
  Button,
  CopyButton,
  Heading,
  HelpText,
  HGrid,
  HStack,
  Link,
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
import { useForm, UseFormReturn } from "react-hook-form";
import { SlettKorreksjonModal } from "@/components/utbetaling/SlettKorreksjonModal";
import { AvbrytUtbetalingModal } from "@/components/utbetaling/AvbrytUtbetalingModal";
import { AvvisAvbrytUtbetalingModal } from "@/components/utbetaling/AvvisAvbrytUtbetalingModal";
import { useGodkjennAvbrytUtbetaling } from "@/api/utbetaling/mutations";
import { ErrorFieldSummary } from "@/components/skjema/ValideringsfeilOppsummering";
import { ToTrinnsAvbrytelseForklaring } from "@/components/totrinnskontroll/ToTrinnskontrollAvbrytningForklaring";
import { TotrinnsBegrunnelse } from "@/components/totrinnskontroll/TotrinnsBegrunnelse";
import { aarsakTilTekst } from "@/utils/Utils";

function useUtbetalingDetaljerData() {
  const { utbetalingId } = useRequiredParams(["utbetalingId"]);
  const { utbetaling, handlinger, tilAvbrytning } = useUtbetaling(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);
  return { utbetaling, handlinger, beregning, tilAvbrytning };
}

export function UtbetalingDetaljerPage() {
  const navigate = useNavigate();
  const godkjennAvbyrtUtbetalingMutation = useGodkjennAvbrytUtbetaling();
  const [errors, setErrors] = useState<FieldError[]>([]);
  const [modalVariant, setModalVariant] = useState<UtbetalingHandling | null>(null);

  const { utbetaling, handlinger, beregning, tilAvbrytning } = useUtbetalingDetaljerData();
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
          pris: linje.pris,
          id: linje.id,
          tilsagnId: linje.tilsagn.id,
        })),
        begrunnelseMindreBetalt: null,
      },
    });

  function godkjennAvbytUtbetaling() {
    godkjennAvbyrtUtbetalingMutation.mutate(
      { id: utbetaling.id },
      {
        onValidationError: (error: ValidationError) => {
          setErrors(error.errors);
        },
        onSuccess: () => {
          navigate(-1);
        },
      },
    );
  }

  return (
    <VStack>
      <HStack justify="end" className="pb-6">
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
              ],
            },
            {
              items: [
                {
                  handling: UtbetalingHandling.SEND_TIL_AVBRYTNING,
                  label: utbetalingTekster.avbrutt.handling.sendTilAvbrytning.label,
                  onClick: () => setModalVariant(UtbetalingHandling.SEND_TIL_AVBRYTNING),
                  variant: "danger",
                  icon: <XMarkIcon />,
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
      </HStack>
      <VStack gap="space-12">
        {utbetaling.status.type !== UtbetalingStatusDtoType.AVBRUTT && tilAvbrytning && (
          <ToTrinnsAvbrytelseForklaring avbrytelse={tilAvbrytning} />
        )}
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
            {utbetaling.status.type === UtbetalingStatusDtoType.AVBRUTT && tilAvbrytning && (
              <>
                <Separator />
                <TotrinnsBegrunnelse
                  title="Begrunnelse for avbrytelse"
                  aarsaker={tilAvbrytning.aarsaker.map((arsak) =>
                    aarsakTilTekst(arsak as UtbetalingStatusAarsak),
                  )}
                  forklaring={tilAvbrytning.forklaring}
                />
              </>
            )}
          </TwoColumnGrid>
        </HGrid>
        <UtbetalingBeregningView utbetalingId={utbetaling.id} beregning={beregning} />
        <UtbetalingLinjeView
          utbetaling={utbetaling}
          utbetalingLinjer={utbetalingLinjer}
          handlinger={handlinger}
          form={form}
        />
        <HStack gap="space-8" justify={"end"}>
          <ErrorFieldSummary errors={errors} />
          {handlinger.includes(UtbetalingHandling.AVVIS_AVBRYTNING) && (
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setModalVariant(UtbetalingHandling.AVVIS_AVBRYTNING)}
            >
              {utbetalingTekster.avbrutt.handling.avvis.label}
            </Button>
          )}
          {handlinger.includes(UtbetalingHandling.GODKJENN_AVBRYTNING) && (
            <Button
              size="small"
              variant="primary"
              type="button"
              onClick={() => godkjennAvbytUtbetaling()}
            >
              {utbetalingTekster.avbrutt.handling.godkjenn.label}
            </Button>
          )}

          <AvbrytUtbetalingModal
            utbetalingId={utbetaling.id}
            open={modalVariant === UtbetalingHandling.SEND_TIL_AVBRYTNING}
            onClose={() => setModalVariant(null)}
          />
          <AvvisAvbrytUtbetalingModal
            utbetalingId={utbetaling.id}
            open={modalVariant === UtbetalingHandling.AVVIS_AVBRYTNING}
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
    case UtbetalingStatusDtoType.TIL_AVBRYTNING:
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
