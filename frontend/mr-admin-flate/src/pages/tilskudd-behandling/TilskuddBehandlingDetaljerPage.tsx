import {
  useGodkjennTilskuddBehandling,
  useReturnerTilskuddBehandling,
} from "@/api/tilskudd-behandling/mutations";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";
import { SaksopplysningerDetaljer } from "@/components/tilskudd-behandling/SaksopplysningerDetaljer";
import { VedtakDetaljer } from "@/components/tilskudd-behandling/VedtakDetaljer";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import {
  FieldError,
  TilskuddBehandlingHandling,
  TilskuddBehandlingStatus,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { ActionMenu, Alert, Button, HStack, Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { TilskuddBehandlingLayout } from "@/components/tilskudd-behandling/TilskuddBehandlingLayout";
import { EndringshistorikkPopover } from "@/components/endringshistorikk/EndringshistorikkPopover";
import { ViewEndringshistorikk } from "@/components/endringshistorikk/ViewEndringshistorikk";
import { Handlinger } from "@/components/handlinger/Handlinger";

export function TilskuddBehandlingDetaljerPage() {
  const { gjennomforingId, behandlingId } = useRequiredParams(["gjennomforingId", "behandlingId"]);
  const { data: behandling } = useTilskuddBehandling(behandlingId);
  const [currentTab, setCurrentTab] = useState<"saksopplysninger" | "vedtak">("saksopplysninger");
  const [returModalOpen, setReturModalOpen] = useState(false);
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

  function sendIRetur(data: { aarsaker: string[]; forklaring: string | null }) {
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

  const erTilGodkjenning = behandling.status.type === TilskuddBehandlingStatus.TIL_GODKJENNING;
  const historikk = { entries: [] };

  return (
    <TilskuddBehandlingLayout
      opprettelse={behandling.opprettelse}
      gjennomforingId={gjennomforingId}
      currentTab={currentTab}
      onTabChange={setCurrentTab}
      tabList={
        <HStack align="center" className="w-full" justify="space-between">
          <HStack>
            <Tabs.Tab value="saksopplysninger" label="Saksopplysninger" />
            <Tabs.Tab value="vedtak" label="Vedtak" />
          </HStack>
          <HStack gap="space-8">
            <EndringshistorikkPopover>
              <ViewEndringshistorikk historikk={historikk} />
            </EndringshistorikkPopover>
            <Handlinger>
              {behandling.handlinger.includes(TilskuddBehandlingHandling.REDIGER) && (
                <ActionMenu.Item onSelect={() => navigate("rediger")}>Rediger</ActionMenu.Item>
              )}
            </Handlinger>
          </HStack>
        </HStack>
      }
      saksopplysningerContent={<SaksopplysningerDetaljer behandling={behandling} />}
      vedtakContent={<VedtakDetaljer behandling={behandling} />}
      actions={
        <>
          {erTilGodkjenning && (
            <HStack gap="space-8" marginBlock="space-16" justify="end">
              {behandling.handlinger.includes(TilskuddBehandlingHandling.RETURNER) && (
                <Button
                  variant="secondary"
                  size="small"
                  type="button"
                  onClick={() => setReturModalOpen(true)}
                >
                  Send i retur
                </Button>
              )}
              {behandling.handlinger.includes(TilskuddBehandlingHandling.ATTESTER) && (
                <Button variant="primary" size="small" type="button" onClick={attester}>
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
          <AarsakerOgForklaringModal<string>
            aarsaker={[{ value: "ANNET", label: "Annet" }]}
            header="Send i retur med forklaring"
            buttonLabel="Send i retur"
            open={returModalOpen}
            onClose={() => setReturModalOpen(false)}
            errors={errors}
            onConfirm={sendIRetur}
          />
        </>
      }
    />
  );
}
