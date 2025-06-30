import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
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
import { formaterDato, navnEllerIdent, tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  Besluttelse,
  BesluttTilsagnRequest,
  FieldError,
  Rolle,
  TilsagnAvvisningAarsak,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TilsagnTilAnnulleringRequest,
  ValidationError,
} from "@mr/api-client-v2";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import {
  EraserIcon,
  PencilFillIcon,
  PiggybankFillIcon,
  TrashFillIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import { ActionMenu, Alert, Button, Heading, HStack, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { aktiveTilsagnQuery, tilsagnHistorikkQuery, tilsagnQuery } from "./tilsagnDetaljerLoader";
import { TilsagnTabell } from "../tabell/TilsagnTabell";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { TilsagnDetaljer } from "./TilsagnDetaljer";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { Laster } from "@/components/laster/Laster";

function useTilsagnDetaljer() {
  const { gjennomforingId, tilsagnId } = useParams();

  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: tilsagnDetaljer } = useApiSuspenseQuery(tilsagnQuery(tilsagnId));
  const { data: ansatt } = useHentAnsatt();
  const { data: historikk } = useApiSuspenseQuery({ ...tilsagnHistorikkQuery(tilsagnId) });
  const { data: aktiveTilsagn } = useApiSuspenseQuery({
    ...aktiveTilsagnQuery(gjennomforingId),
  });
  return {
    ansatt,
    gjennomforing,
    historikk,
    ...tilsagnDetaljer,
    aktiveTilsagn,
  };
}

const tilAnnuleringAarsaker = [
  TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
  TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
  TilsagnTilAnnulleringAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES,
  TilsagnTilAnnulleringAarsak.FEIL_ANNET,
].map((aarsak) => ({
  value: aarsak,
  label: tilsagnAarsakTilTekst(aarsak),
}));

export function TilsagnPage() {
  const {
    ansatt,
    gjennomforing,
    tilsagn,
    opprettelse,
    annullering,
    tilOppgjor,
    historikk,
    aktiveTilsagn,
  } = useTilsagnDetaljer();

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
  const [error, setError] = useState<FieldError[]>([]);

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

  function besluttTilsagn(request: BesluttTilsagnRequest) {
    besluttMutation.mutate(
      {
        id: tilsagn.id,
        body: {
          ...request,
        },
      },
      {
        onSuccess: navigerTilbake,
        onValidationError: (error: ValidationError) => {
          setError(error.errors);
        },
      },
    );
  }

  function tilAnnullering(request: TilsagnTilAnnulleringRequest) {
    tilAnnulleringMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      { onSuccess: navigerTilbake },
    );
  }

  function upsertTilOppgjor(request: TilsagnTilAnnulleringRequest) {
    tilOppgjorMutation.mutate(
      {
        id: tilsagn.id,
        aarsaker: request.aarsaker,
        forklaring: request.forklaring || null,
      },
      { onSuccess: navigerTilbake },
    );
  }

  function slettTilsagn() {
    slettMutation.mutate({ id: tilsagn.id }, { onSuccess: navigerTilbake });
  }
  if (!tilsagn) {
    return <Laster tekst="Laster tilsagn..." />;
  }

  const visHandlingerMeny =
    ansatt.roller.includes(Rolle.SAKSBEHANDLER_OKONOMI) &&
    [TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.status);

  const handlingsMeny = (
    <HStack gap="2" justify={"end"}>
      <EndringshistorikkPopover>
        <ViewEndringshistorikk historikk={historikk} />
      </EndringshistorikkPopover>
      {visHandlingerMeny ? (
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
      ) : null}
    </HStack>
  );

  return (
    <main>
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
          {annullering?.type === "TIL_BESLUTNING" && (
            <AarsakerOgForklaring
              heading="Tilsagnet annulleres"
              tekster={[
                `${navnEllerIdent(annullering.behandletAv)} sendte tilsagnet til annullering den ${formaterDato(
                  annullering.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={
                annullering.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                ) ?? []
              }
              forklaring={annullering.forklaring}
            />
          )}
          {annullering?.type === "BESLUTTET" &&
            annullering.besluttelse === "AVVIST" &&
            !tilOppgjor && (
              <AarsakerOgForklaring
                heading="Annullering avvist"
                tekster={[
                  `${navnEllerIdent(annullering.besluttetAv)} avviste annullering den ${formaterDato(
                    annullering.behandletTidspunkt,
                  )}.`,
                ]}
                aarsaker={
                  annullering.aarsaker?.map((aarsak) =>
                    tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                  ) ?? []
                }
                forklaring={annullering.forklaring}
              />
            )}
          {tilOppgjor?.type === "TIL_BESLUTNING" && (
            <AarsakerOgForklaring
              heading="Tilsagnet gjøres opp"
              ingress="Gjenstående beløp gjøres opp uten at det gjøres en utbetaling"
              tekster={[
                `${navnEllerIdent(tilOppgjor.behandletAv)} sendte tilsagnet til oppgjør den ${formaterDato(
                  tilOppgjor.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={
                tilOppgjor.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                ) ?? []
              }
              forklaring={tilOppgjor.forklaring}
            />
          )}
          {tilOppgjor?.type === "BESLUTTET" && tilOppgjor.besluttelse === "AVVIST" && (
            <AarsakerOgForklaring
              heading="Oppgjør avvist"
              tekster={[
                `${navnEllerIdent(tilOppgjor.besluttetAv)} avviste oppgjør den ${formaterDato(
                  tilOppgjor.behandletTidspunkt,
                )}.`,
              ]}
              aarsaker={
                tilOppgjor.aarsaker?.map((aarsak) =>
                  tilsagnAarsakTilTekst(aarsak as TilsagnTilAnnulleringAarsak),
                ) ?? []
              }
              forklaring={tilOppgjor.forklaring}
            />
          )}
          <VStack gap="6" padding="4" className="rounded-lg border-gray-300 border-1">
            <TilsagnDetaljer
              tilsagn={tilsagn}
              opprettelse={opprettelse}
              annullering={annullering}
              oppgjor={tilOppgjor}
              meny={handlingsMeny}
            />
            <HStack gap="2" justify={"end"}>
              {opprettelse.type === "TIL_BESLUTNING" && (
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
                      onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                    >
                      Godkjenn tilsagn
                    </Button>
                  )}
                </>
              )}
              {annullering?.type === "TIL_BESLUTNING" && (
                <>
                  <Button
                    variant="secondary"
                    size="small"
                    type="button"
                    onClick={() => setAvvisAnnulleringModalOpen(true)}
                  >
                    Avslå annullering
                  </Button>
                  {annullering?.kanBesluttes && (
                    <Button
                      size="small"
                      variant="danger"
                      type="button"
                      onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                    >
                      Bekreft annullering
                    </Button>
                  )}
                </>
              )}
              {tilOppgjor?.type === "TIL_BESLUTNING" && (
                <>
                  <Button
                    variant="secondary"
                    size="small"
                    type="button"
                    onClick={() => setAvvisOppgjorModalOpen(true)}
                  >
                    Avslå oppgjør
                  </Button>
                  {tilOppgjor?.kanBesluttes && (
                    <Button
                      size="small"
                      variant="danger"
                      type="button"
                      onClick={() => besluttTilsagn({ besluttelse: Besluttelse.GODKJENT })}
                    >
                      Bekreft oppgjør
                    </Button>
                  )}
                </>
              )}
            </HStack>
            <AarsakerOgForklaringModal<TilsagnTilAnnulleringAarsak>
              aarsaker={tilAnnuleringAarsaker}
              header="Annuller tilsagn med forklaring"
              buttonLabel="Send til godkjenning"
              open={tilAnnulleringModalOpen}
              onClose={() => setTilAnnulleringModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => tilAnnullering({ aarsaker, forklaring })}
            />
            <AarsakerOgForklaringModal<TilsagnTilAnnulleringAarsak>
              aarsaker={[
                {
                  value: TilsagnTilAnnulleringAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
                  label: "Arrangør har ikke sendt krav",
                },
                { value: TilsagnTilAnnulleringAarsak.FEIL_ANNET, label: "Annet" },
              ]}
              header="Gjør opp tilsagn med forklaring"
              ingress="Gjenstående beløp gjøres opp uten at det gjøres en utbetaling"
              buttonLabel="Send til godkjenning"
              open={tilOppgjorModalOpen}
              onClose={() => setTilOppgjorModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => upsertTilOppgjor({ aarsaker, forklaring })}
            />
            <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
              aarsaker={[
                {
                  value: TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER,
                  label: "Feil i antall plasser",
                },
                {
                  value: TilsagnAvvisningAarsak.FEIL_KOSTNADSSTED,
                  label: "Feil kostnadssted",
                },
                { value: TilsagnAvvisningAarsak.FEIL_PERIODE, label: "Feil periode" },
                { value: TilsagnAvvisningAarsak.FEIL_BELOP, label: "Feil beløp" },
                { value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" },
              ]}
              header="Send i retur med forklaring"
              buttonLabel="Send i retur"
              open={avvisModalOpen}
              onClose={() => setAvvisModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  aarsaker,
                  forklaring,
                });
                setAvvisModalOpen(false);
              }}
            />
            <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
              aarsaker={[{ value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" }]}
              header="Avslå annullering med forklaring"
              buttonLabel="Avslå annullering"
              open={avvisAnnulleringModalOpen}
              onClose={() => setAvvisAnnulleringModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  aarsaker,
                  forklaring,
                });
                setAvvisAnnulleringModalOpen(false);
              }}
            />
            <AarsakerOgForklaringModal<TilsagnAvvisningAarsak>
              aarsaker={[{ value: TilsagnAvvisningAarsak.FEIL_ANNET, label: "Annet" }]}
              header="Avslå oppgjør med forklaring"
              buttonLabel="Avslå oppgjør"
              open={avvisOppgjorModalOpen}
              onClose={() => setAvvisOppgjorModalOpen(false)}
              onConfirm={({ aarsaker, forklaring }) => {
                besluttTilsagn({
                  besluttelse: Besluttelse.AVVIST,
                  aarsaker,
                  forklaring,
                });
                setAvvisOppgjorModalOpen(false);
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
            {error.find((f) => f.pointer === "/") && (
              <Alert className="self-end" variant="error" size="small">
                {error.find((f) => f.pointer === "/")!.detail}
              </Alert>
            )}
          </VStack>
        </VStack>
      </ContentBox>
      <VStack padding="4" className="bg-white overflow-x-scroll">
        <Heading size="medium">Aktive tilsagn</Heading>
        {aktiveTilsagn.length > 0 ? (
          <TilsagnTabell tilsagn={aktiveTilsagn} />
        ) : (
          <Alert variant="info" className="mt-4">
            Det finnes ikke flere aktive tilsagn for dette tiltaket i Nav Tiltaksadministrasjon
          </Alert>
        )}
      </VStack>
    </main>
  );
}
