import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakBeskrivelseForm } from "@/components/tiltak-beskrivelse/TiltakBeskrivelseForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Button, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { useUpsertTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useUpsertTiltakBeskrivelse";
import { TiltakBeskrivelseFormInput, TiltakBeskrivelseSchema } from "./TiltakBeskrivelseFormValues";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakBeskrivelse } from "@/api/tiltak-beskrivelse/useTiltakBeskrivelse";
import {
  Faneinnhold,
  KontorstrukturKontortype,
  TiltakBeskrivelseDto,
  TiltakBeskrivelseDtoAdministrator,
  TiltakBeskrivelseDtoArrangorKontaktperson,
  TiltakBeskrivelseDtoKontaktperson,
  ValidationError,
} from "@tiltaksadministrasjon/api-client";
import { TiltakBeskrivelseIkon } from "@/components/ikoner/TiltakBeskrivelseIkon";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerTiltakBeskrivelsePage() {
  const { tiltakBeskrivelseId } = useRequiredParams(["tiltakBeskrivelseId"]);
  const { data: tiltakBeskrivelse } = useTiltakBeskrivelse(tiltakBeskrivelseId);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksbeskrivelser", lenke: "/tiltak-beskrivelser" },
    {
      tittel: tiltakBeskrivelse.navn,
      lenke: `/tiltak-beskrivelser/${tiltakBeskrivelse.id}`,
    },
    { tittel: "Rediger" },
  ];

  return (
    <>
      <title>{`Rediger | ${tiltakBeskrivelse.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakBeskrivelseIkon />} heading={tiltakBeskrivelse.navn} />
      <ContentBox>
        <WhitePaddedBox>
          <RedigerForm tiltakBeskrivelse={tiltakBeskrivelse} />
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}

function toDefaultValues(ig: TiltakBeskrivelseDto): TiltakBeskrivelseFormInput {
  const navRegioner = ig.veilederinfo.kontorstruktur.map((k) => k.region.enhetsnummer);
  const navKontorer = ig.veilederinfo.kontorstruktur
    .flatMap((k) => k.kontorer)
    .filter((k) => k.type === KontorstrukturKontortype.LOKAL)
    .map((k) => k.enhetsnummer);
  const navAndreEnheter = ig.veilederinfo.kontorstruktur
    .flatMap((k) => k.kontorer)
    .filter((k) => k.type === KontorstrukturKontortype.SPESIALENHET)
    .map((k) => k.enhetsnummer);

  return {
    navn: ig.navn,
    tiltaksnummer: ig.tiltaksnummer ?? null,
    tiltakstypeId: ig.tiltakstype.id,
    stedForGjennomforing: ig.stedForGjennomforing ?? null,
    arrangorId: ig.arrangor?.id ?? null,
    arrangorKontaktpersoner: ig.arrangorKontaktpersoner.map(
      (kp: TiltakBeskrivelseDtoArrangorKontaktperson) => kp.id,
    ),
    administratorer: ig.administratorer.map((a: TiltakBeskrivelseDtoAdministrator) => a.navIdent),
    veilederinformasjon: {
      beskrivelse: ig.veilederinfo.beskrivelse ?? null,
      faneinnhold: ig.veilederinfo.faneinnhold ?? null,
      navRegioner,
      navKontorer,
      navAndreEnheter,
      kontaktpersoner: ig.veilederinfo.kontaktpersoner.map(
        (kp: TiltakBeskrivelseDtoKontaktperson) => ({
          navIdent: kp.navIdent,
          beskrivelse: kp.beskrivelse ?? null,
        }),
      ),
    },
  };
}

function RedigerForm({ tiltakBeskrivelse }: { tiltakBeskrivelse: TiltakBeskrivelseDto }) {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakBeskrivelse();

  const form = useForm<TiltakBeskrivelseFormInput>({
    resolver: zodResolver(TiltakBeskrivelseSchema),
    defaultValues: toDefaultValues(tiltakBeskrivelse),
  });

  const onSubmit: SubmitHandler<TiltakBeskrivelseFormInput> = (data) => {
    upsert.mutate(
      {
        id: tiltakBeskrivelse.id,
        navn: data.navn,
        tiltaksnummer: data.tiltaksnummer ?? null,
        tiltakstypeId: data.tiltakstypeId,
        stedForGjennomforing: data.stedForGjennomforing ?? null,
        arrangorId: data.arrangorId ?? null,
        arrangorKontaktpersoner: data.arrangorKontaktpersoner ?? [],
        administratorer: data.administratorer,
        veilederinformasjon: {
          beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
          faneinnhold: (data.veilederinformasjon.faneinnhold as Faneinnhold | null) ?? null,
          navRegioner: data.veilederinformasjon.navRegioner ?? [],
          navKontorer: data.veilederinformasjon.navKontorer ?? [],
          navAndreEnheter: data.veilederinformasjon.navAndreEnheter ?? [],
          kontaktpersoner:
            data.veilederinformasjon.kontaktpersoner?.map((k) => ({
              navIdent: k.navIdent,
              beskrivelse: k.beskrivelse ?? null,
            })) ?? [],
        },
      },
      {
        onSuccess: () => navigate(`/tiltak-beskrivelser/${tiltakBeskrivelse.id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(form, error),
      },
    );
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)}>
        <VStack gap="space-16">
          <TiltakBeskrivelseForm />
          <div className="flex gap-4">
            <Button type="submit" size="small" disabled={upsert.isPending}>
              {upsert.isPending ? "Lagrer..." : "Lagre"}
            </Button>
            <Button
              type="button"
              variant="tertiary"
              size="small"
              onClick={() => navigate(`/tiltak-beskrivelser/${tiltakBeskrivelse.id}`)}
            >
              Avbryt
            </Button>
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}
