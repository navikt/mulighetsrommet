import { useBesluttTilsagn } from "@/api/tilsagn/useBesluttTilsagn";
import { useSlettTilsagn } from "@/api/tilsagn/useSlettTilsagn";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/useTilsagnTilAnnullering";
import { useTilsagnTilOppgjor } from "@/api/tilsagn/useTilsagnTilOppgjor";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import { FieldError, TilsagnStatus, ValidationError } from "@mr/api-client-v2";
import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  Besluttelse,
  BesluttTotrinnskontrollRequestTilsagnStatusAarsak,
  TilsagnHandling,
  TilsagnStatusAarsak,
} from "@tiltaksadministrasjon/api-client";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { EraserIcon, PencilFillIcon, TrashFillIcon, TrashIcon } from "@navikt/aksel-icons";
import {
  ActionMenu,
  BodyShort,
  Box,
  Button,
  Heading,
  HGrid,
  HStack,
  Spacer,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { useTilsagn, useTilsagnEndringshistorikk } from "./tilsagnDetaljerLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { getAgentDisplayName, isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import {
  MetadataFritekstfelt,
  MetadataHorisontal,
  Separator,
} from "@/components/detaljside/Metadata";
import { TilsagnRegnestykke } from "@/components/tilsagn/beregning/TilsagnRegnestykke";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";
import { DataDetails } from "@/components/data-element/DataDetails";
import { formaterNOK } from "@mr/frontend-common/utils/utils";

function useTilsagnDetaljer(tilsagnId: string) {
  const { data: tilsagnDetaljer } = useTilsagn(tilsagnId);
  const { data: historikk } = useTilsagnEndringshistorikk(tilsagnId);
  return { historikk, ...tilsagnDetaljer };
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

export function TilsagnDetaljer() {
  const { tilsagnId } = useRequiredParams(["tilsagnId"]);

  const { tilsagn, beregning, opprettelse, annullering, tilOppgjor, historikk, handlinger } =
    useTilsagnDetaljer(tilsagnId);

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
      {[TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.status) && (
        <ActionMenu>
          <ActionMenu.Trigger>
            <Button variant="secondary" size="small">
              Handlinger
            </Button>
          </ActionMenu.Trigger>
          <ActionMenu.Content>
            {handlinger.includes(TilsagnHandling.REDIGER) && (
              <ActionMenu.Item icon={<PencilFillIcon />}>
                <Link className="no-underline" to="./rediger-tilsagn">
                  Rediger tilsagn
                </Link>
              </ActionMenu.Item>
            )}
            {handlinger.includes(TilsagnHandling.SLETT) && (
              <ActionMenu.Item
                variant="danger"
                onSelect={() => setSlettTilsagnModalOpen(true)}
                icon={<TrashIcon />}
              >
                Slett tilsagn
              </ActionMenu.Item>
            )}
            {handlinger.includes(TilsagnHandling.ANNULLER) && (
              <ActionMenu.Item
                variant="danger"
                onSelect={() => setTilAnnulleringModalOpen(true)}
                icon={<EraserIcon />}
              >
                Annuller tilsagn
              </ActionMenu.Item>
            )}
            {handlinger.includes(TilsagnHandling.GJOR_OPP) && (
              <ActionMenu.Item
                variant="danger"
                onSelect={() => setTilOppgjorModalOpen(true)}
                icon={<EraserIcon />}
              >
                Gjør opp tilsagn
              </ActionMenu.Item>
            )}
          </ActionMenu.Content>
        </ActionMenu>
      )}
    </HStack>
  );

  const { bestillingsnummer, status, periode, type, kostnadssted, kommentar } = tilsagn;

  return (
    <>
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
        <>
          <HStack className="mb-2">
            <Heading size="medium" level="3">
              Tilsagn
            </Heading>
            <Spacer />
            {handlingsMeny}
          </HStack>
          <TwoColumnGrid separator>
            <HGrid columns={1} gap="2">
              <HGrid columns={{ xl: 2 }} gap="4">
                <VStack gap="4" className="flex-1">
                  <MetadataHorisontal
                    header={tilsagnTekster.bestillingsnummer.label}
                    value={bestillingsnummer}
                  />
                  <MetadataHorisontal
                    header={tilsagnTekster.kostnadssted.label}
                    value={`${kostnadssted.enhetsnummer} ${kostnadssted.navn}`}
                  />
                  <MetadataHorisontal
                    header={tilsagnTekster.periode.label}
                    value={formaterPeriode(periode)}
                  />
                </VStack>
                <VStack gap="4" className="flex-1">
                  <MetadataHorisontal
                    header={tilsagnTekster.status.label}
                    value={<TilsagnTag status={status} />}
                  />
                  <MetadataHorisontal
                    header={tilsagnTekster.type.label}
                    value={avtaletekster.tilsagn.type(type)}
                  />
                </VStack>
                <MetadataFritekstfelt header={tilsagnTekster.kommentar.label} value={kommentar} />
              </HGrid>
              <Separator />
              <DataDetails {...beregning.prismodell} />
            </HGrid>
            <HGrid columns={1} gap="2" align="center">
              <VStack gap="4">
                <MetadataHorisontal
                  header={tilsagnTekster.beregning.belop.label}
                  value={formaterNOK(beregning.belop)}
                />
                <MetadataHorisontal
                  header={tilsagnTekster.belopBrukt.label}
                  value={formaterNOK(tilsagn.belopBrukt)}
                />
                <MetadataHorisontal
                  header={tilsagnTekster.belopGjenstaende.label}
                  value={formaterNOK(tilsagn.belopGjenstaende)}
                />
              </VStack>
              <Separator />
              <Box>
                <Heading size="small" spacing>
                  Beregning
                </Heading>
                <TilsagnRegnestykke regnestykke={beregning.regnestykke} />
              </Box>
              {(status === TilsagnStatus.ANNULLERT || status === TilsagnStatus.OPPGJORT) && (
                <>
                  <Separator />
                  <Heading level="4" spacing size="small">
                    Begrunnelse for {status === TilsagnStatus.ANNULLERT ? "annullering" : "oppgjør"}
                  </Heading>
                  <MetadataHorisontal
                    header={"Årsaker"}
                    value={(tilOppgjor?.aarsaker || annullering?.aarsaker)
                      ?.map((arsak) => tilsagnAarsakTilTekst(arsak as TilsagnStatusAarsak))
                      .join(", ")}
                  />
                  <MetadataFritekstfelt
                    header={"Forklaring"}
                    value={tilOppgjor?.forklaring ?? annullering?.forklaring}
                  />
                </>
              )}
            </HGrid>
          </TwoColumnGrid>
        </>
        <HStack gap="2" justify={"end"}>
          {handlinger.includes(TilsagnHandling.RETURNER) && (
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setAvvisModalOpen(true)}
            >
              Send i retur
            </Button>
          )}
          {handlinger.includes(TilsagnHandling.GODKJENN) && (
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
          {handlinger.includes(TilsagnHandling.AVSLA_ANNULLERING) && (
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setAvvisAnnulleringModalOpen(true)}
            >
              Avslå annullering
            </Button>
          )}
          {handlinger.includes(TilsagnHandling.GODKJENN_ANNULLERING) && (
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
          {handlinger.includes(TilsagnHandling.AVSLA_OPPGJOR) && (
            <Button
              variant="secondary"
              size="small"
              type="button"
              onClick={() => setAvvisOppgjorModalOpen(true)}
            >
              Avslå oppgjør
            </Button>
          )}
          {handlinger.includes(TilsagnHandling.GODKJENN_OPPGJOR) && (
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
              aarsaker,
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
    </>
  );
}
