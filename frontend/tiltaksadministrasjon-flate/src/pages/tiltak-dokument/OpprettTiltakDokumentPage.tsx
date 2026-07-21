import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakDokumentForm } from "@/components/tiltak-dokument/TiltakDokumentForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Button, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { useUpsertTiltakDokument } from "@/api/tiltak-dokument/useUpsertTiltakDokument";
import { TiltakDokumentFormInput, TiltakDokumentSchema } from "./TiltakDokumentFormValues";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { zodResolver } from "@hookform/resolvers/zod";
import { Faneinnhold } from "@tiltaksadministrasjon/api-client";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";

const brodsmuler: Brodsmule[] = [
  { tittel: "Tiltaksdokumenter", lenke: "/tiltak-dokumenter" },
  { tittel: "Opprett tiltaksdokument" },
];

export function OpprettTiltakDokumentPage() {
  const navigate = useNavigate();
  const upsert = useUpsertTiltakDokument();

  const form = useForm<TiltakDokumentFormInput>({
    resolver: zodResolver(TiltakDokumentSchema),
    defaultValues: {
      navn: "",
      tiltakstypeId: "",
      stedForGjennomforing: null,
      arrangorId: null,
      arrangorKontaktpersoner: [],
      administratorer: [],
      veilederinformasjon: {
        beskrivelse: null,
        faneinnhold: null,
        navRegioner: [],
        navKontorer: [],
        navAndreEnheter: [],
        kontaktpersoner: [],
      },
    },
  });

  const onSubmit: SubmitHandler<TiltakDokumentFormInput> = (data) => {
    const id = uuidv4();
    upsert.mutate(
      {
        id,
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
        onSuccess: () => navigate(`/tiltak-dokumenter/${id}`),
      },
    );
  };

  return (
    <>
      <title>Opprett tiltaksdokument</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner ikon={<TiltakDokumentIkon />} heading="Opprett tiltaksdokument" />
      <ContentBox>
        <WhitePaddedBox>
          <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)}>
              <VStack gap="space-16">
                <TiltakDokumentForm />
                <div className="flex gap-4">
                  <Button type="submit" size="small" disabled={upsert.isPending}>
                    {upsert.isPending ? "Oppretter..." : "Opprett"}
                  </Button>
                  <Button
                    type="button"
                    variant="tertiary"
                    size="small"
                    onClick={() => navigate(-1)}
                  >
                    Avbryt
                  </Button>
                </div>
              </VStack>
            </form>
          </FormProvider>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
