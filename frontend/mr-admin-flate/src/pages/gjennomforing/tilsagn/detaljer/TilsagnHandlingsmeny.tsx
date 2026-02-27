import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { PencilFillIcon, TrashIcon, EraserIcon, TrashFillIcon } from "@navikt/aksel-icons";
import { HStack, ActionMenu, Button, BodyShort } from "@navikt/ds-react";
import {
  TilsagnStatus,
  TilsagnHandling,
  TilsagnStatusAarsak,
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  ValidationError,
  FieldError,
} from "@tiltaksadministrasjon/api-client";
import { Link, useNavigate } from "react-router";
import { useTilsagn, useTilsagnEndringshistorikk } from "./tilsagnDetaljerLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useState } from "react";
import { VarselModal } from "@mr/frontend-common/components/varsel/VarselModal";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { useSlettTilsagn } from "@/api/tilsagn/mutations";
import { useTilsagnTilAnnullering } from "@/api/tilsagn/mutations";
import { useTilsagnTilOppgjor } from "@/api/tilsagn/mutations";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";

const tilAnnuleringAarsaker = [
  TilsagnStatusAarsak.ARRANGOR_HAR_IKKE_SENDT_KRAV,
  TilsagnStatusAarsak.FEIL_REGISTRERING,
  TilsagnStatusAarsak.TILTAK_SKAL_IKKE_GJENNOMFORES,
  TilsagnStatusAarsak.ANNET,
].map((aarsak) => ({
  value: aarsak,
  label: tilsagnAarsakTilTekst(aarsak),
}));

export function TilsagnHandlingsmeny() {
  const { tilsagnId } = useRequiredParams(["tilsagnId"]);
  const { data: historikk } = useTilsagnEndringshistorikk(tilsagnId);
  const { data: tilsagn } = useTilsagn(tilsagnId);

  const navigate = useNavigate();
  const tilAnnulleringMutation = useTilsagnTilAnnullering();
  const tilOppgjorMutation = useTilsagnTilOppgjor();
  const slettMutation = useSlettTilsagn();

  const [tilAnnulleringModalOpen, setTilAnnulleringModalOpen] = useState<boolean>(false);
  const [tilOppgjorModalOpen, setTilOppgjorModalOpen] = useState<boolean>(false);
  const [slettTilsagnModalOpen, setSlettTilsagnModalOpen] = useState<boolean>(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  function tilAnnullering(request: AarsakerOgForklaringRequestTilsagnStatusAarsak) {
    tilAnnulleringMutation.mutate(
      { id: tilsagn.tilsagn.id, request },
      {
        onSuccess: () => navigate(-1),
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function upsertTilOppgjor(request: AarsakerOgForklaringRequestTilsagnStatusAarsak) {
    tilOppgjorMutation.mutate(
      { id: tilsagn.tilsagn.id, request },
      {
        onSuccess: () => navigate(-1),
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function slettTilsagn() {
    slettMutation.mutate({ id: tilsagn.tilsagn.id }, { onSuccess: () => navigate(-1) });
  }

  return (
    <HStack gap="space-8" justify={"end"}>
      <EndringshistorikkPopover>
        <ViewEndringshistorikk historikk={historikk} />
      </EndringshistorikkPopover>
      {[TilsagnStatus.RETURNERT, TilsagnStatus.GODKJENT].includes(tilsagn.tilsagn.status.type) && (
        <ActionMenu>
          <ActionMenu.Trigger>
            <Button variant="secondary" size="small">
              Handlinger
            </Button>
          </ActionMenu.Trigger>
          <ActionMenu.Content>
            {tilsagn.handlinger.includes(TilsagnHandling.REDIGER) && (
              <ActionMenu.Item icon={<PencilFillIcon />}>
                <Link className="no-underline" to="./rediger-tilsagn">
                  Rediger tilsagn
                </Link>
              </ActionMenu.Item>
            )}
            {tilsagn.handlinger.includes(TilsagnHandling.SLETT) && (
              <ActionMenu.Item
                variant="danger"
                onSelect={() => setSlettTilsagnModalOpen(true)}
                icon={<TrashIcon />}
              >
                Slett tilsagn
              </ActionMenu.Item>
            )}
            {tilsagn.handlinger.includes(TilsagnHandling.ANNULLER) && (
              <ActionMenu.Item
                variant="danger"
                onSelect={() => setTilAnnulleringModalOpen(true)}
                icon={<EraserIcon />}
              >
                Annuller tilsagn
              </ActionMenu.Item>
            )}
            {tilsagn.handlinger.includes(TilsagnHandling.GJOR_OPP) && (
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
      <VarselModal
        headingIconType="warning"
        headingText="Slette tilsagnet?"
        open={slettTilsagnModalOpen}
        handleClose={() => setSlettTilsagnModalOpen(false)}
        body={
          <p>
            Er du sikker på at du vil slette tilsagnet?
            <br /> Denne operasjonen kan ikke angres
          </p>
        }
        primaryButton={
          <Button
            data-color="danger"
            variant="primary"
            onClick={slettTilsagn}
            icon={<TrashFillIcon />}
          >
            Ja, jeg vil slette tilsagnet
          </Button>
        }
        secondaryButton
        secondaryButtonHandleAction={() => setSlettTilsagnModalOpen(false)}
      />
      <AarsakerOgForklaringModal<TilsagnStatusAarsak>
        aarsaker={tilAnnuleringAarsaker}
        header="Annuller tilsagn med forklaring"
        buttonLabel="Send til godkjenning"
        errors={errors}
        open={tilAnnulleringModalOpen}
        onClose={() => setTilAnnulleringModalOpen(false)}
        onConfirm={tilAnnullering}
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
        onConfirm={upsertTilOppgjor}
      />
    </HStack>
  );
}
