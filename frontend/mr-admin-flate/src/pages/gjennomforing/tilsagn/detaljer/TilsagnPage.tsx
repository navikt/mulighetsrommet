import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useSlettTilsagn } from "@/api/tilsagn/useSlettTilsagn";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/useTilsagnTilAnnullering";
import { useTilsagnTilOppgjor } from "@/api/tilsagn/useTilsagnTilOppgjor";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import { Besluttelse, FieldError, Rolle, TilsagnStatus, ValidationError } from "@mr/api-client-v2";
import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  BesluttTotrinnskontrollRequestTilsagnStatusAarsak,
  TilsagnStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import {
  EraserIcon,
  PencilFillIcon,
  PiggybankFillIcon,
  TrashFillIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import { ActionMenu, BodyShort, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { TilsagnDetaljer } from "./TilsagnDetaljer";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { AktiveTilsagnTable } from "@/pages/gjennomforing/tilsagn/tabell/TilsagnTable";
import { HarTilgang } from "@/components/auth/HarTilgang";
import { useTilsagn, useTilsagnEndringshistorikk } from "./tilsagnDetaljerLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { getAgentDisplayName, isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";

function useTilsagnDetaljer(gjennomforingId: string, tilsagnId: string) {
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: historikk } = useTilsagnEndringshistorikk(tilsagnId);
  return { gjennomforing, historikk, ...tilsagnDetaljer };
}

const tilAnnuleringAarsaker = [
  TilsagnStatusAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
  TilsagnStatusAarsak.FEIL_REGISTRERING,
  TilsagnStatusAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES,
  TilsagnStatusAarsak.ANNET,
].map((aarsak) => ({
  value: aarsak,
  label: tilsagnAarsakTilTekst(aarsak),
}));

export function TilsagnPage() {
  const { gjennomforingId, tilsagnId } = useRequiredParams(["gjennomforingId", "tilsagnId"]);

  const { gjennomforing, tilsagn, beregning, opprettelse, annullering, tilOppgjor, historikk } =
    useTilsagnDetaljer(gjennomforingId, tilsagnId);

  const besluttMutation = useBesluttTilsagn();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const tilOppgjorMutation = useTilsagnTilOppgjor();
  const slettMutation = useSlettTilsagn();

  const navigate = useNavigate();
  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const [tilOppgjorModalOpen, setTilOppgjorModalOpen] = useState<boolean>(false);
  const [slettTilsagnModalOpen, setSlettTilsagnModalOpen] = useState<boolean>(false);
  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [avvisAnnulleringModalOpen, setAvvisAnnulleringModalOpen] = useState(false);
  const [avvisOppgjorModalOpen, setAvvisOppgjorModalOpen] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: `/gjennomforinger`,
    },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Tilsagnsoversikt",
      lenke: `/gjennomforinger/${gjennomforing.id}/tilsagn`,
    },
    {
      tittel: "Tilsagn",
    },
  ];

  function navigerTilbake() {
    navigate(-1);
  }

  function besluttTilsagn(request: BesluttTotrinnskontrollRequestTilsagnStatusAarsak) {
    besluttMutation.mutate(
      {
        id: tilsagn.id,
        body: {
          ...request,
        },
      },
      {
        onSuccess: navigerTilbake,
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function tilAnnullering(request: AarsakerOgForklaringRequestTilsagnStatusAarsak) {
    tilAnnulleringMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      {
        onSuccess: navigerTilbake,
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function upsertTilOppgjor(request: AarsakerOgForklaringRequestTilsagnStatusAarsak) {
    tilOppgjorMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      {
        onSuccess: navigerTilbake,
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function slettTilsagn() {
    slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilbake });
  }

  const handlingsMeny = (
    <HStack gap="2" justify={"end"}>
      <EndringshistorikkPopover>
        <ViewEndringshistorikk historikk={historikk} />
      </EndringshistorikkPopover>
      <HarTilgang
        rolle={Rolle.SAKSBEHANDLER_OKONOMI}
        condition={[TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.status)}
      >
        <ActionMenu>
          <ActionMenu.Trigger>
            <Button variant="secondary" size="small">
              Handlinger
            </Button>
          </ActionMenu.Trigger>
          <ActionMenu.Content>
            {tilsagn.status === TilsagnStatus.RETURNERT && (
              <>
                <ActionMenu.Item icon={<PencilFillIcon />}>
                  <Link className="no-underline" to="./rediger-tilsagn">
                    Rediger tilsagn
                  </Link>
                </ActionMenu.Item>
                <ActionMenu.Item
                  variant="danger"
                  onSelect={() => setSlettTilsagnModalOpen(true)}
                  icon={<TrashIcon />}
                >
                  Slett tilsagn
                </ActionMenu.Item>
              </>
            )}
            {tilsagn.status === TilsagnStatus.GODKJENT &&
              (tilsagn.belopBrukt === 0 ? (
                <>
                  <ActionMenu.Item
                    variant="danger"
                    onSelect={() => setTilAnnulleringModalOpen(true)}
                    icon={<EraserIcon />}
                  >
                    Annuller tilsagn
                  </ActionMenu.Item>
                </>
              ) : (
                <>
                  <ActionMenu.Item
                    variant="danger"
                    onSelect={() => setTilOppgjorModalOpen(true)}
                    icon={<EraserIcon />}
                  >
                    Gjør opp tilsagn
                  </ActionMenu.Item>
                </>
              ))}
          </ActionMenu.Content>
        </ActionMenu>
      </HarTilgang>
    </HStack>
  );

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HStack gap="2" className="bg-white border-b-2 border-gray-200 p-2">
        <PiggybankFillIcon color="#FFAA33" className="w-10 h-10" />
        <Heading size="large" level="2">
          Tilsagn for {gjennomforing.navn}
        </Heading>
      </HStack>
      <ContentBox>
        <VStack gap="6" padding="4" className="bg-white">
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
          {isTilBeslutning(annullering) && (
            <AarsakerOgForklaring
              heading="Tilsagnet annulleres"
              tekster={[
                `${getAgentDisplayName(annullering.behandletAv)} sendte tilsagnet til annullering den ${formaterDato(
                  annullering.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={annullering.aarsaker.map((aarsak) =>
                tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
              )}
              forklaring={annullering.forklaring}
            />
          )}
          {isBesluttet(annullering) && annullering.besluttelse === "AVVIST" && !tilOppgjor && (
            <AarsakerOgForklaring
              heading="Annullering avvist"
              tekster={[
                `${getAgentDisplayName(annullering.besluttetAv)} avviste annullering den ${formaterDato(
                  annullering.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={annullering.aarsaker.map((aarsak) =>
                tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
              )}
              forklaring={annullering.forklaring}
            />
          )}
          {isTilBeslutning(tilOppgjor) && (
            <AarsakerOgForklaring
              heading="Tilsagnet gjøres opp"
              ingress="Gjenstående beløp gjøres opp uten at det gjøres en utbetaling"
              tekster={[
                `${getAgentDisplayName(tilOppgjor.behandletAv)} sendte tilsagnet til oppgjør den ${formaterDato(
                  tilOppgjor.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={tilOppgjor.aarsaker.map((aarsak) =>
                tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
              )}
              forklaring={tilOppgjor.forklaring}
            />
          )}
          {isBesluttet(tilOppgjor) && tilOppgjor.besluttelse === "AVVIST" && (
            <AarsakerOgForklaring
              heading="Oppgjør avvist"
              tekster={[
                `${getAgentDisplayName(tilOppgjor.besluttetAv)} avviste oppgjør den ${formaterDato(
                  tilOppgjor.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={tilOppgjor.aarsaker.map((aarsak) =>
                tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
              )}
              forklaring={tilOppgjor.forklaring}
            />
          )}
          <VStack gap="6" padding="4" className="rounded-lg border-gray-300 border-1">
            <TilsagnDetaljer
              tilsagn={tilsagn}
              beregning={beregning}
              opprettelse={opprettelse}
              annullering={annullering}
              oppgjor={tilOppgjor}
              meny={handlingsMeny}
            />
            <HStack gap="2" justify={"end"}>
              {isTilBeslutning(opprettelse) && (
                <>
                  <Button
                    variant="secondary"
                    size="small"
                    type="button"
                    onClick={() => setAvvisModalOpen(true)}
                  >
                    Send i retur
                  </Button>
                  {opprettelse.kanBesluttes && (
                    <Button
                      size="small"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: Besluttelse.GODKJENT,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
                    >
                      Godkjenn tilsagn
                    </Button>
                  )}
                </>
              )}
              {isTilBeslutning(annullering) && (
                <>
                  <Button
                    variant="secondary"
                    size="small"
                    type="button"
                    onClick={() => setAvvisAnnulleringModalOpen(true)}
                  >
                    Avslå annullering
                  </Button>
                  {annullering.kanBesluttes && (
                    <Button
                      size="small"
                      variant="danger"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: Besluttelse.GODKJENT,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
                    >
                      Bekreft annullering
                    </Button>
                  )}
                </>
              )}
              {isTilBeslutning(tilOppgjor) && (
                <>
                  <Button
                    variant="secondary"
                    size="small"
                    type="button"
                    onClick={() => setAvvisOppgjorModalOpen(true)}
                  >
                    Avslå oppgjør
                  </Button>
                  {tilOppgjor.kanBesluttes && (
                    <Button
                      size="small"
                      variant="danger"
                      type="button"
                      onClick={() =>
                        besluttTilsagn({
                          besluttelse: Besluttelse.GODKJENT,
                          aarsaker: [],
                          forklaring: null,
                        })
                      }
                    >
                      Bekreft oppgjør
                    </Button>
                  )}
                </>
              )}
            </HStack>
            <AarsakerOgForklaringModal<TilsagnStatusAarsak>
              aarsaker={tilAnnuleringAarsaker}
              header="Annuller tilsagn med forklaring"
              buttonLabel="Send til godkjenning"
              errors={errors}
              open={tilAnnulleringModalOpen}
              onClose={() => setTilAnnulleringModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => tilAnnullering({ aarsaker, forklaring })}
            />
            <AarsakerOgForklaringModal<TilsagnStatusAarsak>
              aarsaker={[
                {
                  value: TilsagnStatusAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
                  label: "Arrangør har ikke sendt krav",
                },
                { value: TilsagnStatusAarsak.ANNET, label: "Annet" },
              ]}
              header="Gjør opp tilsagn med forklaring"
              ingress={
                <BodyShort>Gjenstående beløp gjøres opp uten at det gjøres en utbetaling</BodyShort>
              }
              buttonLabel="Send til godkjenning"
              open={tilOppgjorModalOpen}
              onClose={() => setTilOppgjorModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => upsertTilOppgjor({ aarsaker, forklaring })}
            />
            <AarsakerOgForklaringModal<TilsagnStatusAarsak>
              aarsaker={[
                {
                  value: TilsagnStatusAarsak.FEIL_ANTALL_PLASSER,
                  label: "Feil i antall plasser",
                },
                {
                  value: TilsagnStatusAarsak.FEIL_KOSTNADSSTED,
                  label: "Feil kostnadssted",
                },
                { value: TilsagnStatusAarsak.FEIL_PERIODE, label: "Feil periode" },
                { value: TilsagnStatusAarsak.FEIL_BELOP, label: "Feil beløp" },
                { value: TilsagnStatusAarsak.ANNET, label: "Annet" },
              ]}
              header="Send i retur med forklaring"
              buttonLabel="Send i retur"
              open={avvisModalOpen}
              onClose={() => setAvvisModalOpen(false)}
              errors={errors}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  aarsaker,
                  forklaring,
                });
                setAvvisModalOpen(false);
              }}
            />
            <AarsakerOgForklaringModal<TilsagnStatusAarsak>
              aarsaker={[{ value: TilsagnStatusAarsak.ANNET, label: "Annet" }]}
              header="Avslå annullering med forklaring"
              buttonLabel="Avslå annullering"
              open={avvisAnnulleringModalOpen}
              onClose={() => setAvvisAnnulleringModalOpen(false)}
              errors={errors}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  aarsaker,
                  forklaring,
                });
              }}
            />
            <AarsakerOgForklaringModal<TilsagnStatusAarsak>
              aarsaker={[{ value: TilsagnStatusAarsak.ANNET, label: "Annet" }]}
              header="Avslå oppgjør med forklaring"
              buttonLabel="Avslå oppgjør"
              open={avvisOppgjorModalOpen}
              onClose={() => setAvvisOppgjorModalOpen(false)}
              errors={errors}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  // TODO: fix types
                  aarsaker: aarsaker as unknown as TilsagnStatusAarsak[],
                  forklaring,
                });
              }}
            />
            <VarselModal
              headingIconType="warning"
              headingText="Slette tilsagnet?"
              open={slettTilsagnModalOpen}
              handleClose={() => setTilOppgjorModalOpen(false)}
              body={
                <p>
                  Er du sikker på at du vil slette tilsagnet?
                  <br /> Denne operasjonen kan ikke angres
                </p>
              }
              primaryButton={
                <Button variant="danger" onClick={slettTilsagn} icon={<TrashFillIcon />}>
                  Ja, jeg vil slette tilsagnet
                </Button>
              }
              secondaryButton
              secondaryButtonHandleAction={() => setTilOppgjorModalOpen(false)}
            />
          </VStack>
        </VStack>
      </ContentBox>
      <VStack padding="4" className="bg-white overflow-x-scroll">
        <AktiveTilsagnTable gjennomforingId={gjennomforingId} />
      </VStack>
    </>
  );
}
