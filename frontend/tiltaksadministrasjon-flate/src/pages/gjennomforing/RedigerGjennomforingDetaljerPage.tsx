import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGjennomforingByPathParam } from "@/api/gjennomforing/useGjennomforing";
import { useUpdateGjennomforingDetaljer } from "@/api/gjennomforing/useUpdateGjennomforingDetaljer";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { defaultGjennomforingData } from "@/pages/gjennomforing/form/defaults";
import { GjennomforingFormDetaljer } from "@/components/gjennomforing/GjennomforingFormDetaljer";
import {
  GjennomforingDetaljerInputValues,
  GjennomforingDetaljerOutputValues,
  gjennomforingDetaljerSchema,
} from "@/pages/gjennomforing/form/validation";
import { GjennomforingAvtaleDetaljerDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "@/pages/gjennomforing/RedigerGjennomforingPageLayout";
import { toGjennomforingDetaljerRequest } from "./form/mappers";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";
import { isGjennomforingAvtaleDetaljer } from "@/api/gjennomforing/utils";

export function RedigerGjennomforingDetaljerPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGjennomforingAvtaleDetaljer(detaljer)) {
    return null;
  }

  return <RedigerDetaljerForm {...detaljer} />;
}

function RedigerDetaljerForm(detaljer: GjennomforingAvtaleDetaljerDto) {
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(detaljer.gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);
  const { data: deltakere } = useGjennomforingDeltakerSummary(detaljer.gjennomforing.id);

  const mutation = useUpdateGjennomforingDetaljer(detaljer.gjennomforing.id);

  const methods = useForm<
    GjennomforingDetaljerInputValues,
    unknown,
    GjennomforingDetaljerOutputValues
  >({
    resolver: zodResolver(gjennomforingDetaljerSchema),
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
    mutation.mutate(toGjennomforingDetaljerRequest(data), {
      onSuccess: () => navigate(`/gjennomforinger/${detaljer.gjennomforing.id}`),
      onValidationError: (validation: ValidationError) => {
        applyValidationErrors(methods, validation);
      },
    });
  });

  return (
    <RedigerGjennomforingPageLayout>
      <FormContainer heading="Redigerer detaljer" methods={methods} onSubmit={onSubmit}>
        <GjennomforingFormDetaljer
          tiltakstype={tiltakstype}
          avtale={avtale}
          gjennomforing={detaljer.gjennomforing}
          deltakere={deltakere}
        />
      </FormContainer>
    </RedigerGjennomforingPageLayout>
  );
}
