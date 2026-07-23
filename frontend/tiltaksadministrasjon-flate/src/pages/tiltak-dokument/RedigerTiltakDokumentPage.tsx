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
import {
  Faneinnhold,
  KontorstrukturKontortype,
  TiltakDokumentDto,
  TiltakDokumentDtoAdministrator,
  TiltakDokumentDtoArrangorKontaktperson,
  TiltakDokumentDtoKontaktperson,
} from "@tiltaksadministrasjon/api-client";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";

export function RedigerTiltakDokumentPage() {
  const { tiltakDokumentId } = useRequiredParams(["tiltakDokumentId"]);
  const { data: tiltakDokument } = useTiltakDokument(tiltakDokumentId);

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
    {
      tittel: tiltakDokument.navn,
      lenke: `/tiltak-dokumenter/${tiltakDokument.id}`,
    },
    { tittel: "Rediger" },
  ];

  return (
    <>
      <title>{`Rediger | ${tiltakDokument.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakDokumentIkon />} heading={tiltakDokument.navn} />
      <ContentBox>
        <WhitePaddedBox>
          <RedigerForm tiltakDokument={tiltakDokument} />
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}

function toDefaultValues(ig: TiltakDokumentDto): TiltakDokumentFormInput {
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
    tiltakstypeId: ig.tiltakstype.id,
    stedForGjennomforing: ig.stedForGjennomforing ?? null,
    arrangorId: ig.arrangor?.id ?? null,
    arrangorKontaktpersoner: ig.arrangorKontaktpersoner.map(
      (kp: TiltakDokumentDtoArrangorKontaktperson) => kp.id,
    ),
    administratorer: ig.administratorer.map((a: TiltakDokumentDtoAdministrator) => a.navIdent),
    veilederinformasjon: {
      beskrivelse: ig.veilederinfo.beskrivelse ?? null,
      faneinnhold: ig.veilederinfo.faneinnhold ?? null,
      navRegioner,
      navKontorer,
      navAndreEnheter,
      kontaktpersoner: ig.veilederinfo.kontaktpersoner.map(
        (kp: TiltakDokumentDtoKontaktperson) => ({
          navIdent: kp.navIdent,
          beskrivelse: kp.beskrivelse ?? null,
        }),
      ),
    },
  };
}

function RedigerForm({ tiltakDokument }: { tiltakDokument: TiltakDokumentDto }) {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakDokument();

  const form = useForm<TiltakDokumentFormInput>({
    resolver: zodResolver(TiltakDokumentSchema),
    defaultValues: toDefaultValues(tiltakDokument),
  });

  const onSubmit: SubmitHandler<TiltakDokumentFormInput> = (data) => {
    upsert.mutate(
      {
        id: tiltakDokument.id,
        navn: data.navn,
        tiltakstypeId: data.tiltakstypeId,
        stedForGjennomforing: data.stedForGjennomforing ?? null,
        arrangorId: data.arrangorId ?? null,
        arrangorKontaktpersoner: data.arrangorKontaktpersoner ?? [],
        beskrivelse: data.veilederinformasjon.beskrivelse ?? null,
        faneinnhold: (data.veilederinformasjon.faneinnhold as Faneinnhold | null) ?? null,
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
        onSuccess: () => navigate(`/tiltak-dokumenter/${tiltakDokument.id}`),
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
              onClick={() => navigate(`/tiltak-dokumenter/${tiltakDokument.id}`)}
            >
              Avbryt
            </Button>
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}
