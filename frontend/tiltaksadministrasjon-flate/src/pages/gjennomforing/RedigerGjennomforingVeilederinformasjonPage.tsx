import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGjennomforingByPathParam } from "@/api/gjennomforing/useGjennomforing";
import { isGjennomforingAvtaleDetaljer } from "@/api/gjennomforing/utils";
import { useUpdateGjennomforingVeilederinformasjon } from "@/api/gjennomforing/useUpdateGjennomforingVeilederinformasjon";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { defaultGjennomforingData } from "@/pages/gjennomforing/form/defaults";
import { GjennomforingInformasjonForVeiledereForm } from "@/components/gjennomforing/GjennomforingInformasjonForVeiledereForm";
import {
  GjennomforingVeilederinfoInputValues,
  GjennomforingVeilederinfoOutputValues,
  gjennomforingVeilederinfoSchema,
} from "@/pages/gjennomforing/form/validation";
import { GjennomforingAvtaleDetaljerDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "@/pages/gjennomforing/RedigerGjennomforingPageLayout";
import { toGjennomforingVeilederinfoRequest } from "./form/mappers";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerGjennomforingVeilederinformasjonPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGjennomforingAvtaleDetaljer(detaljer)) {
    return null;
  }

  return <RedigerVeilederinformasjonForm {...detaljer} />;
}

function RedigerVeilederinformasjonForm(detaljer: GjennomforingAvtaleDetaljerDto) {
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(detaljer.gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  const mutation = useUpdateGjennomforingVeilederinformasjon(detaljer.gjennomforing.id);

  const methods = useForm<
    GjennomforingVeilederinfoInputValues,
    unknown,
    GjennomforingVeilederinfoOutputValues
  >({
    resolver: zodResolver(gjennomforingVeilederinfoSchema),
    defaultValues: defaultGjennomforingData(
      ansatt,
      tiltakstype,
      avtale,
      detaljer.gjennomforing,
      detaljer.veilederinfo,
      detaljer.prismodell,
      detaljer.opplaring,
    ),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toGjennomforingVeilederinfoRequest(data), {
      onSuccess: () =>
        navigate(`/gjennomforinger/${detaljer.gjennomforing.id}/redaksjonelt-innhold`),
      onValidationError: (validation: ValidationError) => {
        applyValidationErrors(methods, validation);
      },
    });
  });

  return (
    <RedigerGjennomforingPageLayout>
      <FormContainer
        heading="Redigerer informasjon for veiledere"
        methods={methods}
        onSubmit={onSubmit}
      >
        <GjennomforingInformasjonForVeiledereForm
          avtale={avtale}
          veilederinfo={detaljer.veilederinfo}
        />
      </FormContainer>
    </RedigerGjennomforingPageLayout>
  );
}
