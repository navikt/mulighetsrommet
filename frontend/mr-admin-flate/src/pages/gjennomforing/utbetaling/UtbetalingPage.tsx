import React, { useEffect, useReducer, useState } from "react";
import { MetadataFritekstfelt, MetadataHorisontal } from "@/components/detaljside/Metadata";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { FieldError, ValidationError } from "@mr/api-client-v2";
import {
  DelutbetalingRequest,
  OpprettDelutbetalingerRequest,
  Rolle,
  UtbetalingDto,
  UtbetalingHandling,
  UtbetalingLinje,
  UtbetalingStatusDtoType,
} from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BankNoteFillIcon } from "@navikt/aksel-icons";
import {
  Accordion,
  Alert,
  Button,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  VStack,
} from "@navikt/ds-react";
import { useParams } from "react-router";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { BesluttUtbetalingLinjeView } from "@/components/utbetaling/BesluttUtbetalingLinjeView";
import { RedigerUtbetalingLinjeView } from "@/components/utbetaling/RedigerUtbetalingLinjeView";
import { UtbetalingStatusTag } from "@/components/utbetaling/UtbetalingStatusTag";
import { utbetalingTekster } from "@/components/utbetaling/UtbetalingTekster";
import { UtbetalingTypeText } from "@mr/frontend-common/components/utbetaling/UtbetalingTypeTag";
import UtbetalingBeregningView from "@/components/utbetaling/beregning/UtbetalingBeregningView";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { useOpprettDelutbetalinger } from "@/api/utbetaling/useOpprettDelutbetalinger";
import { useQueryClient } from "@tanstack/react-query";
import MindreBelopModal from "@/components/utbetaling/MindreBelopModal";
import { HarTilgang } from "@/components/auth/HarTilgang";
import {
  useUtbetaling,
  useUtbetalingBeregning,
  useUtbetalingEndringshistorikk,
  useUtbetalingsLinjeFraTilsagn,
} from "./utbetalingPageLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { QueryKeys } from "@/api/QueryKeys";
import { UtbetalingLinjerState, UtbetalingLinjerStateAction } from "./helper";

function useUtbetalingPageData() {
  const { gjennomforingId, utbetalingId } = useRequiredParams(["gjennomforingId", "utbetalingId"]);

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: historikk } = useUtbetalingEndringshistorikk(utbetalingId);
  const { data: utbetaling } = useUtbetaling(utbetalingId);
  const { data: utbetalingsLinjerFraTilsagn } = useUtbetalingsLinjeFraTilsagn(utbetalingId);
  const { data: beregning } = useUtbetalingBeregning({ navEnheter: [] }, utbetalingId);

  // @todo: This is quickfix. Figure out why it scrolls to the bottom on page load as a part of the broader frontend improvements
  useEffect(() => {
    window.scrollTo(0, 0); // Reset scroll position to the top
  }, []);

  return {
    gjennomforing,
    historikk,
    utbetalingsLinjerFraTilsagn,
    utbetaling: utbetaling.utbetaling,
    handlinger: utbetaling.handlinger,
    linjer: utbetaling.linjer,
    beregning,
  };
}

export function UtbetalingPage() {
  const { gjennomforingId, utbetalingId } = useParams();
  const {
    gjennomforing,
    historikk,
    utbetalingsLinjerFraTilsagn,
    utbetaling,
    linjer,
    beregning,
    handlinger,
  } = useUtbetalingPageData();
  const opprettMutation = useOpprettDelutbetalinger(utbetaling.id);

  function utbetalingsLinjeInitialState() {
    return { linjer: linjer.length === 0 ? utbetalingsLinjerFraTilsagn : linjer };
  }

  function utbetalingsLinjeReducer(
    state: UtbetalingLinjerState,
    action: UtbetalingLinjerStateAction,
  ): UtbetalingLinjerState {
    switch (action.type) {
      case "RESET":
        return utbetalingsLinjeInitialState();
      case "REMOVE":
        return {
          ...state,
          linjer: state.linjer.filter((l: UtbetalingLinje) => l.id !== action.id),
        };
      case "UPDATE":
        return {
          ...state,
          linjer: state.linjer.map((l: UtbetalingLinje) =>
            l.id === action.linje.id ? action.linje : l,
          ),
        };
      default:
        return state;
    }
  }
  const [linjerState, linjerDispatch] = useReducer(
    utbetalingsLinjeReducer,
    utbetalingsLinjeInitialState(),
  );

  const [errors, setErrors] = useState<FieldError[]>([]);
  const [begrunnelseMindreBetalt, setBegrunnelseMindreBetalt] = useState<string | null>(null);
  const [mindreBelopModalOpen, setMindreBelopModalOpen] = useState<boolean>(false);
  const queryClient = useQueryClient();

  function sendTilGodkjenning() {
    const delutbetalingReq: DelutbetalingRequest[] = linjerState.linjer.map(
      (linje: UtbetalingLinje) => {
        return {
          id: linje.id,
          belop: linje.belop,
          gjorOppTilsagn: linje.gjorOppTilsagn,
          tilsagnId: linje.tilsagn.id,
        };
      },
    );

    const body: OpprettDelutbetalingerRequest = {
      utbetalingId: utbetaling.id,
      delutbetalinger: delutbetalingReq,
      begrunnelseMindreBetalt,
    };

    setErrors([]);

    opprettMutation.mutate(body, {
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: QueryKeys.utbetaling(utbetaling.id),
          refetchType: "all",
        });
      },
      onValidationError: (error: ValidationError) => {
        setErrors(error.errors);
      },
    });
  }

  function utbetalesTotal(): number {
    return linjerState.linjer.reduce((acc: number, d: UtbetalingLinje) => acc + d.belop, 0);
  }

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforingId}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforingId}/utbetalinger`,
    },
    { tittel: "Utbetaling" },
  ];

  return (
    <>
      <title>Utbetalinger</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <BankNoteFillIcon color="#2AA758" className="w-10 h-10" />
        <Heading size="large" level="1">
          Utbetaling for {gjennomforing.navn}
        </Heading>
      </HStack>
      <ContentBox>
        <WhitePaddedBox>
          <VStack gap="4">
            <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
            <VStack
              gap="4"
              id="kostnadsfordeling"
              padding="4"
              className="border-gray-300 border-1 rounded-lg"
            >
              <HGrid columns="1fr 1fr 0.25fr">
                <VStack>
                  <Heading size="medium" level="2" spacing data-testid="utbetaling-til-utbetaling">
                    Til utbetaling
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Status"
                      value={<UtbetalingStatusTag status={utbetaling.status} />}
                    />

                    <MetadataHorisontal
                      header="Utbetalingsperiode"
                      value={formaterPeriode(utbetaling.periode)}
                    />
                    {utbetaling.type.tagName && (
                      <MetadataHorisontal
                        header="Type"
                        value={<UtbetalingTypeText type={utbetaling.type} />}
                      />
                    )}
                    <MetadataHorisontal
                      header="Dato innsendt"
                      value={formaterDato(utbetaling.godkjentAvArrangorTidspunkt)}
                    />
                    <MetadataHorisontal header="Innsendt av" value={utbetaling.innsendtAv} />
                    <MetadataHorisontal
                      header={utbetalingTekster.beregning.belop.label}
                      value={formaterNOK(utbetaling.belop)}
                    />
                    {utbetaling.beskrivelse && (
                      <MetadataFritekstfelt
                        header="Begrunnelse for utbetaling"
                        value={utbetaling.beskrivelse}
                      />
                    )}
                    {utbetaling.begrunnelseMindreBetalt && (
                      <MetadataFritekstfelt
                        header="Begrunnelse for mindre utbetalt"
                        value={utbetaling.begrunnelseMindreBetalt}
                      />
                    )}
                  </VStack>
                </VStack>
                <VStack gap="4">
                  <Heading size="medium" level="2">
                    Betalingsinformasjon
                  </Heading>
                  <VStack gap="2">
                    <MetadataHorisontal
                      header="Kontonummer"
                      value={utbetaling.betalingsinformasjon.kontonummer}
                    />
                    <MetadataHorisontal
                      header="KID (valgfritt)"
                      value={utbetaling.betalingsinformasjon.kid}
                    />
                  </VStack>
                  {utbetaling.journalpostId ? (
                    <>
                      <Heading size="medium" level="2">
                        Journalføring
                      </Heading>
                      <VStack gap="2">
                        <MetadataHorisontal
                          header="Journalpost-ID i Gosys"
                          value={
                            <HStack align="center">
                              <CopyButton
                                size="small"
                                variant="action"
                                copyText={utbetaling.journalpostId}
                                title="Kopier journalpost-ID"
                              />
                              {utbetaling.journalpostId}
                            </HStack>
                          }
                        />
                      </VStack>
                    </>
                  ) : null}
                </VStack>
                <HStack justify="end" align="start">
                  <EndringshistorikkPopover>
                    <ViewEndringshistorikk historikk={historikk} />
                  </EndringshistorikkPopover>
                </HStack>
              </HGrid>
              <>
                <Accordion>
                  <Accordion.Item>
                    <Accordion.Header>Beregning - {beregning.heading}</Accordion.Header>
                    <Accordion.Content>
                      {utbetalingId && (
                        <UtbetalingBeregningView
                          utbetalingId={utbetalingId}
                          beregning={beregning}
                        />
                      )}
                    </Accordion.Content>
                  </Accordion.Item>
                </Accordion>
                {!linjerState.linjer.length && (
                  <Alert variant="info">
                    Det finnes ingen godkjente tilsagn for utbetalingsperioden
                  </Alert>
                )}
                <UtbetalingLinjeView
                  utbetaling={utbetaling}
                  linjer={linjerState.linjer}
                  linjerDispatch={linjerDispatch}
                />
              </>
              <VStack gap="2">
                <HStack justify="end">
                  {handlinger.includes(UtbetalingHandling.SEND_TIL_ATTESTERING) && (
                    <Button
                      size="small"
                      type="button"
                      onClick={() => {
                        if (utbetalesTotal() < utbetaling.belop) {
                          setMindreBelopModalOpen(true);
                        } else {
                          sendTilGodkjenning();
                        }
                      }}
                    >
                      Send til attestering
                    </Button>
                  )}
                </HStack>
                <VStack gap="2" align="end">
                  {errors.map((error) => (
                    <Alert variant="error" size="small">
                      {error.detail}
                    </Alert>
                  ))}
                </VStack>
              </VStack>
            </VStack>
          </VStack>
        </WhitePaddedBox>
      </ContentBox>
      <MindreBelopModal
        open={mindreBelopModalOpen}
        handleClose={() => setMindreBelopModalOpen(false)}
        onConfirm={() => {
          setMindreBelopModalOpen(false);
          sendTilGodkjenning();
        }}
        begrunnelseOnChange={(e: any) => setBegrunnelseMindreBetalt(e.target.value)}
        belopUtbetaling={utbetalesTotal()}
        belopInnsendt={utbetaling.belop}
      />
    </>
  );
}

interface UtbetalingLinjeViewProps {
  utbetaling: UtbetalingDto;
  linjer: UtbetalingLinje[];
  linjerDispatch: React.ActionDispatch<[action: UtbetalingLinjerStateAction]>;
}

function UtbetalingLinjeView({ utbetaling, linjer, linjerDispatch }: UtbetalingLinjeViewProps) {
  switch (utbetaling.status.type) {
    case UtbetalingStatusDtoType.VENTER_PA_ARRANGOR:
      return null;
    case UtbetalingStatusDtoType.RETURNERT:
    case UtbetalingStatusDtoType.KLAR_TIL_BEHANDLING:
      return (
        <HarTilgang rolle={Rolle.SAKSBEHANDLER_OKONOMI}>
          <RedigerUtbetalingLinjeView
            linjerDispatch={linjerDispatch}
            utbetaling={utbetaling}
            linjer={linjer}
          />
        </HarTilgang>
      );
    case UtbetalingStatusDtoType.TIL_ATTESTERING:
    case UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING:
      return (
        <HarTilgang rolle={Rolle.ATTESTANT_UTBETALING}>
          <BesluttUtbetalingLinjeView utbetaling={utbetaling} linjer={linjer} />
        </HarTilgang>
      );
  }
}
