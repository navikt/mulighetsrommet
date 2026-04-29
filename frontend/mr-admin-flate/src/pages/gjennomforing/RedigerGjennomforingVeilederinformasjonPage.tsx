import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  useGjennomforing,
  useGjennomforingByPathParam,
} from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
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
import { GjennomforingAvtaleDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "@/pages/gjennomforing/RedigerGjennomforingPageLayout";
import { toGjennomforingVeilederinfoRequest } from "./form/mappers";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";

export function RedigerGjennomforingVeilederinformasjonPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGruppetiltak(detaljer.gjennomforing)) {
    return null;
  }

  return (
    <RedigerVeilederinformasjonForm
      gjennomforingId={detaljer.gjennomforing.id}
      gjennomforing={detaljer.gjennomforing}
    />
  );
}

interface FormProps {
  gjennomforingId: string;
  gjennomforing: GjennomforingAvtaleDto;
}

function RedigerVeilederinformasjonForm({ gjennomforingId, gjennomforing }: FormProps) {
  const navigate = useNavigate();
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = useAvtale(gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);

  const mutation = useUpdateGjennomforingVeilederinformasjon(gjennomforingId);

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
      gjennomforing,
      detaljer.veilederinfo,
      detaljer.prismodell,
      detaljer.amoKategorisering,
      detaljer.utdanningslop,
    ),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toGjennomforingVeilederinfoRequest(data), {
      onSuccess: () => navigate(`/gjennomforinger/${gjennomforingId}/redaksjonelt-innhold`),
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
