import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakDokumentForm } from "@/components/tiltak-dokument/TiltakDokumentForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Button, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { useUpsertTiltakDokument } from "@/api/tiltak-dokument/useUpsertTiltakDokument";
import { TiltakDokumentFormInput, TiltakDokumentSchema } from "./TiltakDokumentFormValues";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { useTiltakDokument } from "@/api/tiltak-dokument/useTiltakDokument";
import { TiltakDokument } from "@/api/tiltak-dokument/useTiltakDokumenter";
import { KontorstrukturKontortype } from "@tiltaksadministrasjon/api-client";

export function RedigerTiltakDokumentPage() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: gjennomforing } = useTiltakDokument(tiltakDokumentId);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
    {
      tittel: gjennomforing.navn,
      lenke: `/tiltak-dokumenter/${gjennomforing.id}`,
    },
    { tittel: "Rediger" },
  ];

  return (
    <>
      <title>{`Rediger | ${gjennomforing.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<GjennomforingAvtaleIkon />} heading={gjennomforing.navn} />
      <ContentBox>
        <WhitePaddedBox>
          <RedigerForm gjennomforing={gjennomforing} />
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}

function toDefaultValues(ig: TiltakDokument): TiltakDokumentFormInput {
  const navRegioner = ig.kontorstruktur.map((k) => k.region.enhetsnummer);
  const navKontorer = ig.kontorstruktur
    .flatMap((k) => k.kontorer)
    .filter((k) => k.type === KontorstrukturKontortype.LOKAL)
    .map((k) => k.enhetsnummer);
  const navAndreEnheter = ig.kontorstruktur
    .flatMap((k) => k.kontorer)
    .filter((k) => k.type === KontorstrukturKontortype.SPESIALENHET)
    .map((k) => k.enhetsnummer);

  return {
    navn: ig.navn,
    tiltakstypeId: ig.tiltakstype.id,
    stedForGjennomforing: ig.stedForGjennomforing ?? null,
    arrangorId: ig.arrangor?.id ?? null,
    arrangorKontaktpersoner: ig.arrangorKontaktpersoner.map((kp) => kp.id),
    administratorer: ig.administratorer.map((a) => a.navIdent),
    veilederinformasjon: {
      beskrivelse: ig.beskrivelse ?? null,
      faneinnhold: ig.faneinnhold ?? null,
      navRegioner,
      navKontorer,
      navAndreEnheter,
      kontaktpersoner: ig.kontaktpersoner.map((kp) => ({
        navIdent: kp.navIdent,
        beskrivelse: kp.beskrivelse ?? null,
      })),
    },
  };
}

function RedigerForm({ gjennomforing }: { gjennomforing: TiltakDokument }) {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakDokument();

  const form = useForm<TiltakDokumentFormInput>({
    resolver: zodResolver(TiltakDokumentSchema),
    defaultValues: toDefaultValues(gjennomforing),
  });

  const onSubmit: SubmitHandler<TiltakDokumentFormInput> = (data) => {
    upsert.mutate(
      {
        id: gjennomforing.id,
        navn: data.navn,
        tiltakstypeId: data.tiltakstypeId,
        stedForGjennomforing: data.stedForGjennomforing ?? null,
        arrangorId: data.arrangorId ?? null,
        arrangorKontaktpersoner: data.arrangorKontaktpersoner ?? [],
        beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
        faneinnhold: data.veilederinformasjon.faneinnhold ?? null,
        administratorer: data.administratorer,
        navRegioner: data.veilederinformasjon.navRegioner ?? [],
        navKontorer: data.veilederinformasjon.navKontorer ?? [],
        navAndreEnheter: data.veilederinformasjon.navAndreEnheter ?? [],
        kontaktpersoner:
          data.veilederinformasjon.kontaktpersoner?.map((k) => ({
            navIdent: k.navIdent,
            beskrivelse: k.beskrivelse ?? null,
          })) ?? [],
      },
      {
        onSuccess: () => navigate(`/tiltak-dokumenter/${gjennomforing.id}`),
      },
    );
  };

  return (
    <FormProvider {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)}>
        <VStack gap="space-16">
          <TiltakDokumentForm />
          <div className="flex gap-4">
            <Button type="submit" size="small" disabled={upsert.isPending}>
              {upsert.isPending ? "Lagrer..." : "Lagre"}
            </Button>
            <Button
              type="button"
              variant="tertiary"
              size="small"
              onClick={() => navigate(`/tiltak-dokumenter/${gjennomforing.id}`)}
            >
              Avbryt
            </Button>
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}
