import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  AarsakerOgForklaringRequestTilsagnStatusAarsak,
  FieldError,
  TilsagnHandling,
  TilsagnStatus,
  TilsagnStatusAarsak,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { Box, Button, Heading, HGrid, HStack, Show, Spacer, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { AarsakerOgForklaring } from "../AarsakerOgForklaring";
import { ToTrinnsOpprettelsesForklaring } from "../ToTrinnsOpprettelseForklaring";
import { formaterDato, formaterPeriode } from "@mr/frontend-common/utils/date";
import { useTilsagn } from "./tilsagnDetaljerLoader";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { isBesluttet, isTilBeslutning } from "@/utils/totrinnskontroll";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnRegnestykke } from "@/components/tilsagn/beregning/TilsagnRegnestykke";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";
import { formaterValuta } from "@mr/frontend-common/utils/utils";
import {
  MetadataFritekstfelt,
  MetadataVStack,
  Separator,
} from "@mr/frontend-common/components/datadriven/Metadata";
import { getDataElement } from "@mr/frontend-common";
import { useGodkjennTilsagn, useReturnerTilsagn } from "@/api/tilsagn/mutations";
import { TilsagnHandlingsmeny } from "./TilsagnHandlingsmeny";

export function TilsagnDetaljer() {
  const { tilsagnId } = useRequiredParams(["tilsagnId"]);

  const { data } = useTilsagn(tilsagnId);
  const { tilsagn, beregning, annullering, tilOppgjor, opprettelse, handlinger } = data;

  const godkjennTilsagnMutation = useGodkjennTilsagn();
  const returnerTilsagnMutation = useReturnerTilsagn();

  const navigate = useNavigate();

  const [avvisModalOpen, setAvvisModalOpen] = useState(false);
  const [avvisAnnulleringModalOpen, setAvvisAnnulleringModalOpen] = useState(false);
  const [avvisOppgjorModalOpen, setAvvisOppgjorModalOpen] = useState(false);
  const [errors, setErrors] = useState<FieldError[]>([]);

  function godkjennTilsagn() {
    godkjennTilsagnMutation.mutate(
      { id: tilsagn.id },
      {
        onSuccess: () => navigate(-1),
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }

  function returnerTilsagn(request: AarsakerOgForklaringRequestTilsagnStatusAarsak) {
    returnerTilsagnMutation.mutate(
      { id: tilsagn.id, request },
      {
        onSuccess: () => navigate(-1),
        onValidationError: (error: ValidationError) => setErrors(error.errors),
      },
    );
  }
  const { bestillingsnummer, status, periode, type, kostnadssted, kommentar, beskrivelse } =
    tilsagn;

  return (
    <>
      <ToTrinnsOpprettelsesForklaring opprettelse={opprettelse} />
      {isTilBeslutning(annullering) && (
        <AarsakerOgForklaring
          heading="Tilsagnet annulleres"
          tekster={[
            `${annullering.behandletAv.navn} sendte tilsagnet til annullering den ${formaterDato(
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
            `${annullering.besluttetAv.navn} avviste annullering den ${formaterDato(
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
            `${tilOppgjor.behandletAv.navn} sendte tilsagnet til oppgjør den ${formaterDato(
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
            `${tilOppgjor.besluttetAv.navn} avviste oppgjør den ${formaterDato(
              tilOppgjor.behandletTidspunkt,
            )}.`,
          ]}
          aarsaker={tilOppgjor.aarsaker.map((aarsak) =>
            tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
          )}
          forklaring={tilOppgjor.forklaring}
        />
      )}
      <VStack gap="space-24" padding="space-16" className="rounded-lg border-ax-neutral-400 border">
        <HStack>
          <Heading size="medium" level="3">
            Tilsagn
          </Heading>
          <Spacer />
          <TilsagnHandlingsmeny />
        </HStack>
        <TwoColumnGrid separator>
          <HGrid columns={1} gap="space-8">
            <HGrid columns={{ xl: 2 }} gap="space-16">
              <VStack gap="space-16" className="flex-1">
                <MetadataVStack
                  label={tilsagnTekster.bestillingsnummer.label}
                  value={bestillingsnummer}
                />
                <MetadataVStack
                  label={tilsagnTekster.kostnadssted.label}
                  value={`${kostnadssted.enhetsnummer} ${kostnadssted.navn}`}
                />
                <MetadataVStack
                  label={tilsagnTekster.periode.label}
                  value={formaterPeriode(periode)}
                />
              </VStack>
              <VStack gap="space-16" className="flex-1">
                <MetadataVStack
                  label={tilsagnTekster.status.label}
                  value={<TilsagnTag status={status} />}
                />
                <MetadataVStack
                  label={tilsagnTekster.type.label}
                  value={avtaletekster.tilsagn.type(type)}
                />
              </VStack>
            </HGrid>
            <Separator />
            <VStack gap="space-16" className="flex-1">
              <MetadataFritekstfelt label={tilsagnTekster.kommentar.label} value={kommentar} />

              <MetadataFritekstfelt label={tilsagnTekster.beskrivelse.label} value={beskrivelse} />
            </VStack>
            <Show below="lg">
              <Separator />
            </Show>
          </HGrid>
          <HGrid columns={1} gap="space-8" align="center">
            {beregning.prismodell.entries.map((entry) => (
              <MetadataVStack
                key={entry.label}
                label={entry.label}
                value={entry.value ? getDataElement(entry.value) : "-"}
              />
            ))}
            <Separator />
            <VStack gap="space-16">
              <MetadataVStack
                label={tilsagnTekster.beregning.belop.label}
                value={formaterValuta(beregning.pris.belop, beregning.pris.valuta)}
              />
              <MetadataVStack
                label={tilsagnTekster.belopBrukt.label}
                value={formaterValuta(tilsagn.belopBrukt.belop, tilsagn.belopBrukt.valuta)}
              />
              <MetadataVStack
                label={tilsagnTekster.belopGjenstaende.label}
                value={formaterValuta(
                  tilsagn.belopGjenstaende.belop,
                  tilsagn.belopGjenstaende.valuta,
                )}
              />
            </VStack>
            <Separator />
            <Box>
              <Heading size="small" spacing>
                Beregning
              </Heading>
              <TilsagnRegnestykke regnestykke={beregning.regnestykke} />
            </Box>
            {[TilsagnStatus.ANNULLERT, TilsagnStatus.OPPGJORT].includes(status.type) && (
              <>
                <Separator />
                <Heading level="4" spacing size="small">
                  {status.type === TilsagnStatus.ANNULLERT
                    ? "Begrunnelse for annullering"
                    : "Begrunnelse for oppgjør"}
                </Heading>
                <MetadataVStack
                  label={"Årsaker"}
                  value={(tilOppgjor?.aarsaker || annullering?.aarsaker)
                    ?.map((arsak) => tilsagnAarsakTilTekst(arsak as TilsagnStatusAarsak))
                    .join(", ")}
                />
                <MetadataFritekstfelt
                  label={"Forklaring"}
                  value={tilOppgjor?.forklaring ?? annullering?.forklaring}
                />
              </>
            )}
          </HGrid>
        </TwoColumnGrid>
        <HStack gap="space-8" justify={"end"}>
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
            <Button size="small" type="button" onClick={godkjennTilsagn}>
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
              data-color="danger"
              size="small"
              variant="primary"
              type="button"
              onClick={godkjennTilsagn}
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
              data-color="danger"
              size="small"
              variant="primary"
              type="button"
              onClick={godkjennTilsagn}
            >
              Bekreft oppgjør
            </Button>
          )}
        </HStack>
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
          onConfirm={returnerTilsagn}
        />
        <AarsakerOgForklaringModal<TilsagnStatusAarsak>
          aarsaker={[{ value: TilsagnStatusAarsak.ANNET, label: "Annet" }]}
          header="Avslå annullering med forklaring"
          buttonLabel="Avslå annullering"
          open={avvisAnnulleringModalOpen}
          onClose={() => setAvvisAnnulleringModalOpen(false)}
          errors={errors}
          onConfirm={returnerTilsagn}
        />
        <AarsakerOgForklaringModal<TilsagnStatusAarsak>
          aarsaker={[{ value: TilsagnStatusAarsak.ANNET, label: "Annet" }]}
          header="Avslå oppgjør med forklaring"
          buttonLabel="Avslå oppgjør"
          open={avvisOppgjorModalOpen}
          onClose={() => setAvvisOppgjorModalOpen(false)}
          errors={errors}
          onConfirm={returnerTilsagn}
        />
      </VStack>
    </>
  );
}
