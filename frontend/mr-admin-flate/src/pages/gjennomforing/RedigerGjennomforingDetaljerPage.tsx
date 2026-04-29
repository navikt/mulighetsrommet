import { useAvtale } from "@/api/avtaler/useAvtale";
import {
  useGjennomforing,
  useGjennomforingByPathParam,
} from "@/api/gjennomforing/useGjennomforing";
import { isGruppetiltak } from "@/api/gjennomforing/utils";
import { useUpdateGjennomforingDetaljer } from "@/api/gjennomforing/useUpdateGjennomforingDetaljer";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormDetaljer } from "@/components/gjennomforing/GjennomforingFormDetaljer";
import {
  GjennomforingDetaljerInputValues,
  GjennomforingDetaljerOutputValues,
  gjennomforingDetaljerSchema,
} from "@/schemas/gjennomforing";
import { GjennomforingAvtaleDto, ValidationError } from "@tiltaksadministrasjon/api-client";
import { useNavigate } from "react-router";
import { RedigerGjennomforingPageLayout } from "@/pages/gjennomforing/RedigerGjennomforingPageLayout";
import { toGjennomforingDetaljerRequest } from "./gjennomforingFormUtils";
import { FormContainer } from "@/components/skjema/FormContainer";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { useGjennomforingDeltakerSummary } from "@/api/gjennomforing/useGjennomforingDeltakerSummary";

export function RedigerGjennomforingDetaljerPage() {
  const detaljer = useGjennomforingByPathParam();

  if (!isGruppetiltak(detaljer.gjennomforing)) {
    return null;
  }

  return (
    <RedigerDetaljerForm
      gjennomforingId={detaljer.gjennomforing.id}
      gjennomforing={detaljer.gjennomforing}
    />
  );
}

interface FormProps {
  gjennomforingId: string;
  gjennomforing: GjennomforingAvtaleDto;
}

function RedigerDetaljerForm({ gjennomforingId, gjennomforing }: FormProps) {
  const navigate = useNavigate();
  const detaljer = useGjennomforing(gjennomforingId);
  const { data: avtale } = useAvtale(gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const tiltakstype = useTiltakstype(detaljer.tiltakstype.id);
  const { data: deltakere } = useGjennomforingDeltakerSummary(gjennomforingId);

  const mutation = useUpdateGjennomforingDetaljer(gjennomforingId);

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
      gjennomforing,
      detaljer.veilederinfo,
      detaljer.prismodell,
      detaljer.amoKategorisering,
      detaljer.utdanningslop,
    ),
  });

  const onSubmit = methods.handleSubmit((data) => {
    mutation.mutate(toGjennomforingDetaljerRequest(data), {
      onSuccess: () => navigate(`/gjennomforinger/${gjennomforingId}`),
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
          gjennomforing={gjennomforing}
          deltakere={deltakere}
        />
      </FormContainer>
    </RedigerGjennomforingPageLayout>
  );
}
