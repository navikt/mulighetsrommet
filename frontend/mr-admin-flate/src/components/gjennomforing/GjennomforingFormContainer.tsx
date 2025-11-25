import { gjennomforingDetaljerTabAtom } from "@/api/atoms";
import { useUpsertGjennomforing } from "@/api/gjennomforing/useUpsertGjennomforing";
import { Laster } from "@/components/laster/Laster";
import {
  AvtaleDto,
  GjennomforingDeltakerSummary,
  GjennomforingDto,
  GjennomforingRequest,
  NavEnhetDto,
  Tiltakskode,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Box, Spacer, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai";
import React, { useCallback } from "react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { v4 as uuidv4 } from "uuid";
import { TabWithErrorBorder } from "@/components/skjema/TabWithErrorBorder";
import { GjennomforingFormDetaljer } from "./GjennomforingFormDetaljer";
import { GjennomforingFormKnapperad } from "./GjennomforingFormKnapperad";
import { GjennomforingInformasjonForVeiledereForm } from "./GjennomforingInformasjonForVeiledereForm";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";

interface Props {
  onClose: () => void;
  onSuccess: (id: string) => void;
  avtale: AvtaleDto;
  gjennomforing: GjennomforingDto | null;
  deltakere: GjennomforingDeltakerSummary | null;
  defaultValues: DeepPartial<GjennomforingRequest>;
  enheter: NavEnhetDto[];
}

export function GjennomforingFormContainer(props: Props) {
  const { avtale, gjennomforing, deltakere, defaultValues, onClose, onSuccess } = props;
  const redigeringsModus = !!gjennomforing;
  const mutation = useUpsertGjennomforing();
  const [activeTab, setActiveTab] = useAtom(gjennomforingDetaljerTabAtom);

  const form = useForm<GjennomforingRequest>({
    resolver: async (values) => ({ values, errors: {} }),
    defaultValues,
  });

  const {
    handleSubmit,
    formState: { errors },
  } = form;

  const handleSuccess = useCallback(
    (dto: { data: GjennomforingDto }) => onSuccess(dto.data.id),
    [onSuccess],
  );

  const postData: SubmitHandler<GjennomforingRequest> = async (data): Promise<void> => {
    const body: GjennomforingRequest = {
      id: gjennomforing?.id || uuidv4(),
      antallPlasser: data.antallPlasser,
      tiltakstypeId: avtale.tiltakstype.id,
      veilederinformasjon: {
        navRegioner: data.veilederinformasjon.navRegioner,
        navKontorer: data.veilederinformasjon.navKontorer,
        navAndreEnheter: data.veilederinformasjon.navAndreEnheter,
        beskrivelse: data.veilederinformasjon.beskrivelse,
        faneinnhold: data.veilederinformasjon.faneinnhold
          ? {
              forHvemInfoboks: data.veilederinformasjon.faneinnhold.forHvemInfoboks || null,
              forHvem: data.veilederinformasjon.faneinnhold.forHvem || null,
              detaljerOgInnholdInfoboks:
                data.veilederinformasjon.faneinnhold.detaljerOgInnholdInfoboks || null,
              detaljerOgInnhold: data.veilederinformasjon.faneinnhold.detaljerOgInnhold || null,
              pameldingOgVarighetInfoboks:
                data.veilederinformasjon.faneinnhold.pameldingOgVarighetInfoboks || null,
              pameldingOgVarighet: data.veilederinformasjon.faneinnhold.pameldingOgVarighet || null,
              kontaktinfo: data.veilederinformasjon.faneinnhold.kontaktinfo || null,
              kontaktinfoInfoboks: data.veilederinformasjon.faneinnhold.kontaktinfoInfoboks || null,
              lenker: data.veilederinformasjon.faneinnhold.lenker || null,
              oppskrift: data.veilederinformasjon.faneinnhold.oppskrift || null,
              delMedBruker: data.veilederinformasjon.faneinnhold.delMedBruker || null,
            }
          : null,
      },
      navn: data.navn,
      startDato: data.startDato,
      sluttDato: data.sluttDato || null,
      avtaleId: avtale.id,
      administratorer: data.administratorer,
      arrangorId: data.arrangorId,
      oppstart: data.oppstart,
      kontaktpersoner: data.kontaktpersoner
        .filter((kontakt) => kontakt.navIdent !== "")
        .map((kontakt) => ({
          navIdent: kontakt.navIdent,
          beskrivelse: kontakt.beskrivelse ?? null,
        })),
      stedForGjennomforing: data.stedForGjennomforing,
      oppmoteSted: data.oppmoteSted,
      arrangorKontaktpersoner: data.arrangorKontaktpersoner,
      deltidsprosent: data.deltidsprosent,
      estimertVentetid: data.estimertVentetid ?? null,
      tilgjengeligForArrangorDato: data.tilgjengeligForArrangorDato ?? null,
      amoKategorisering: data.amoKategorisering ?? null,
      utdanningslop:
        avtale.tiltakstype.tiltakskode === Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
          ? (data.utdanningslop ?? null)
          : null,
    };

    mutation.mutate(body, {
      onSuccess: handleSuccess,
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((error) => {
          const name = jsonPointerToFieldPath(error.pointer) as keyof GjennomforingRequest;
          form.setError(name, { type: "custom", message: error.detail });
        });
      },
    });
  };

  const hasRedaksjoneltInnholdErrors = Boolean(errors.veilederinformasjon?.faneinnhold);
  const hasDetaljerErrors = Object.keys(errors).length > (hasRedaksjoneltInnholdErrors ? 1 : 0);

  return (
    <FormProvider {...form}>
      <form onSubmit={handleSubmit(postData)}>
        <Tabs defaultValue={activeTab}>
          <Tabs.List className="flex flex-row justify-between">
            <TabWithErrorBorder
              onClick={() => setActiveTab("detaljer")}
              value="detaljer"
              label="Detaljer"
              hasError={hasDetaljerErrors}
            />
            <TabWithErrorBorder
              onClick={() => setActiveTab("redaksjonelt-innhold")}
              value="redaksjonelt-innhold"
              label="Informasjon for veiledere"
              hasError={hasRedaksjoneltInnholdErrors}
            />
            <Spacer />
            <GjennomforingFormKnapperad
              redigeringsModus={redigeringsModus}
              onClose={onClose}
              isPending={mutation.isPending}
            />
          </Tabs.List>
          <Tabs.Panel value="detaljer">
            <InlineErrorBoundary>
              <React.Suspense fallback={<Laster tekst="Laster innhold" />}>
                <Box marginBlock="4">
                  <GjennomforingFormDetaljer
                    avtale={avtale}
                    gjennomforing={gjennomforing}
                    deltakere={deltakere}
                  />
                </Box>
              </React.Suspense>
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <Box marginBlock="4">
              <GjennomforingInformasjonForVeiledereForm
                avtale={avtale}
                lagredeKontaktpersoner={gjennomforing?.kontaktpersoner ?? []}
              />
            </Box>
          </Tabs.Panel>
        </Tabs>
        <Separator />
        <GjennomforingFormKnapperad
          redigeringsModus={redigeringsModus}
          onClose={onClose}
          isPending={mutation.isPending}
        />
      </form>
    </FormProvider>
  );
}
