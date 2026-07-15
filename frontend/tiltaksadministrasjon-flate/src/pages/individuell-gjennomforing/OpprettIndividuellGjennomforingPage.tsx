import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { IndividuellGjennomforingForm } from "@/components/individuell-gjennomforing/IndividuellGjennomforingForm";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Button, VStack } from "@navikt/ds-react";
import { FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { useUpsertIndividuellGjennomforing } from "@/api/individuell-gjennomforing/useUpsertIndividuellGjennomforing";
import {
  IndividuellGjennomforingFormInput,
  IndividuellGjennomforingSchema,
} from "./IndividuellGjennomforingFormValues";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { zodResolver } from "@hookform/resolvers/zod";

const brodsmuler: Brodsmule[] = [
  { tittel: "Individuelle gjennomføringer", lenke: "/individuelle-gjennomforinger" },
  { tittel: "Opprett individuell gjennomføring" },
];

export function OpprettIndividuellGjennomforingPage() {
  const navigate = useNavigate();
  const upsert = useUpsertIndividuellGjennomforing();

  const form = useForm<IndividuellGjennomforingFormInput>({
    resolver: zodResolver(IndividuellGjennomforingSchema),
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

  const onSubmit: SubmitHandler<IndividuellGjennomforingFormInput> = (data) => {
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
        onSuccess: () => navigate(`/individuelle-gjennomforinger/${id}`),
      },
    );
  };

  return (
    <>
      <title>Opprett individuell gjennomføring</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner
        ikon={<GjennomforingAvtaleIkon />}
        heading="Opprett individuell gjennomføring"
      />
      <ContentBox>
        <WhitePaddedBox>
          <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)}>
              <VStack gap="space-16">
                <IndividuellGjennomforingForm />
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
