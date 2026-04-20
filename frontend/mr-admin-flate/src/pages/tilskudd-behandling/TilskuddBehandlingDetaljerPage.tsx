import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import {
  useGodkjennTilskuddBehandling,
  useReturnerTilskuddBehandling,
} from "@/api/tilskudd-behandling/mutations";
import { useTilskuddBehandling } from "@/api/tilskudd-behandling/useTilskuddBehandling";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { SaksopplysningerDetaljer } from "@/components/tilskudd-behandling/SaksopplysningerDetaljer";
import { VedtakDetaljer } from "@/components/tilskudd-behandling/VedtakDetaljer";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import {
  FieldError,
  TilskuddBehandlingStatus,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Alert, Button, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { AarsakerOgForklaringModal } from "@/components/modal/AarsakerOgForklaringModal";

type Tab = "saksopplysninger" | "vedtak";

export function TilskuddBehandlingDetaljerPage() {
  const { gjennomforingId, behandlingId } = useRequiredParams(["gjennomforingId", "behandlingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const { data: behandling } = useTilskuddBehandling(behandlingId);
  const [currentTab, setCurrentTab] = useState<Tab>("saksopplysninger");
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

  return (
    <>
      <title>Tilskuddsbehandling</title>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
          { tittel: "Gjennomføring", lenke: `/gjennomforinger/${gjennomforingId}` },
          {
            tittel: "Tilskuddsbehandlinger",
            lenke: `/gjennomforinger/${gjennomforingId}/tilskudd-behandling` as const,
          },
          { tittel: "Behandling" },
        ]}
      />
      <Header>
        <GavelSoundBlockFillIcon
          color="var(--ax-text-brand-blue-decoration)"
          aria-hidden
          width="2.5rem"
          height="2.5rem"
        />
        <Heading size="large" level="2">
          Tilskuddsbehandling
        </Heading>
      </Header>
      <WhitePaddedBox>
        <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
        <Tabs value={currentTab} onChange={(value) => setCurrentTab(value as Tab)}>
          <Tabs.List>
            <Tabs.Tab value="saksopplysninger" label="Saksopplysninger" />
            <Tabs.Tab value="vedtak" label="Vedtak" />
          </Tabs.List>
          <TwoColumnGrid separator>
            <div>
              <Tabs.Panel value="saksopplysninger">
                <SaksopplysningerDetaljer behandling={behandling} />
              </Tabs.Panel>
              <Tabs.Panel value="vedtak">
                <VedtakDetaljer behandling={behandling} />
              </Tabs.Panel>
            </div>
            <Heading size="medium" level="3" spacing>
              Oppsummering
            </Heading>
          </TwoColumnGrid>
        </Tabs>
        <Separator />
        <HStack gap="space-8" marginBlock="space-16" justify="end">
          {erTilGodkjenning && (
            <>
              <Button
                variant="secondary"
                size="small"
                type="button"
                onClick={() => setReturModalOpen(true)}
              >
                Send i retur
              </Button>
              <Button variant="primary" size="small" type="button" onClick={attester}>
                Attester
              </Button>
            </>
          )}
        </HStack>
        {errors.map((error) => (
          <Alert className="self-end" variant="error" size="small">
            {error.detail}
          </Alert>
        ))}
      </WhitePaddedBox>
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
  );
}
